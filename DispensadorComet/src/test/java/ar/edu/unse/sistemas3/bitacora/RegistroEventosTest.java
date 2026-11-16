package ar.edu.unse.sistemas3.bitacora;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RegistroEventos: bitácora del banco de pruebas")
class RegistroEventosTest {

    @Test
    @DisplayName("Conserva el orden cronológico y el origen de cada suceso")
    void conservaOrdenYOrigen() {
        RegistroEventos registro = new RegistroEventos();

        registro.info("Sensor", "primero");
        registro.alerta("Balanza", "segundo");
        registro.falla("Cerradura", "tercero");

        List<RegistroEventos.Entrada> entradas = registro.entradas();
        assertEquals(3, entradas.size());
        assertEquals("primero", entradas.get(0).mensaje());
        assertEquals(RegistroEventos.Nivel.ALERTA, entradas.get(1).nivel());
        assertEquals("Cerradura", entradas.get(2).origen());
        assertEquals(1, registro.cantidad(RegistroEventos.Nivel.FALLA));
    }

    @Test
    @DisplayName("Notifica a los suscriptores cada suceso nuevo")
    void notificaSuscriptores() {
        RegistroEventos registro = new RegistroEventos();
        AtomicInteger recibidos = new AtomicInteger();
        registro.suscribir(entrada -> recibidos.incrementAndGet());

        registro.info("A", "uno");
        registro.info("B", "dos");

        assertEquals(2, recibidos.get());
    }

    @Test
    @DisplayName("Soporta escrituras concurrentes sin perder sucesos")
    void soportaEscriturasConcurrentes() throws InterruptedException {
        RegistroEventos registro = new RegistroEventos();
        int hilos = 8;
        int porHilo = 200;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch arranque = new CountDownLatch(1);

        for (int i = 0; i < hilos; i++) {
            final int id = i;
            pool.submit(() -> {
                listos.countDown();
                try {
                    arranque.await();
                    for (int j = 0; j < porHilo; j++) {
                        registro.info("hilo-" + id, "suceso " + j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await();
        arranque.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(hilos * porHilo, registro.cantidad());
    }

    @Test
    @DisplayName("La entrada se formatea con hora, nivel y origen")
    void formateaLaEntrada() {
        RegistroEventos registro = new RegistroEventos();
        registro.alerta("SensorPosicionTapaInput", "lectura incoherente");

        String texto = registro.entradas().get(0).toString();

        assertTrue(texto.contains("ALERTA"));
        assertTrue(texto.contains("SensorPosicionTapaInput"));
        assertTrue(texto.contains("lectura incoherente"));
        assertTrue(texto.matches(".*\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*"));
    }
}
