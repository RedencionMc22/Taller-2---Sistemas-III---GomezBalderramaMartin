package ar.edu.unse.sistemas3.dispositivos;

import ar.edu.unse.sistemas3.eventos.EstadoTapa;
import ar.edu.unse.sistemas3.eventos.EventoTapa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SensorPosicionTapa: dispositivo de entrada simulado (actividad 1)")
class SensorPosicionTapaTest {

    @Test
    @DisplayName("Arranca cerrado, como la tapa al iniciar el sistema")
    void arrancaCerrado() {
        SensorPosicionTapa sensor = new SensorPosicionTapa();

        assertEquals(EstadoTapa.CERRADA, sensor.estado());
        assertEquals(0, sensor.transiciones());
    }

    @Test
    @DisplayName("Al abrir publica el evento «tapa abierta» con su instante")
    void abrirPublicaEventoTapaAbierta() {
        SensorPosicionTapa sensor = new SensorPosicionTapa();
        List<EventoTapa> recibidos = new ArrayList<>();
        sensor.agregarEscucha(recibidos::add);

        Instant antes = Instant.now();
        sensor.abrir();
        Instant despues = Instant.now();

        assertEquals(1, recibidos.size());
        EventoTapa evento = recibidos.get(0);
        assertEquals(EstadoTapa.ABIERTA, evento.estado());
        assertTrue(evento.esCoherente());
        assertFalse(evento.instante().isBefore(antes));
        assertFalse(evento.instante().isAfter(despues));
        assertEquals(EstadoTapa.ABIERTA, sensor.estado());
        assertEquals(1, sensor.transiciones());
    }

    @Test
    @DisplayName("Al cerrar publica el evento «tapa cerrada»")
    void cerrarPublicaEventoTapaCerrada() {
        SensorPosicionTapa sensor = new SensorPosicionTapa();
        List<EventoTapa> recibidos = new ArrayList<>();
        sensor.agregarEscucha(recibidos::add);

        sensor.abrir();
        sensor.cerrar();

        assertEquals(2, recibidos.size());
        assertEquals(EstadoTapa.ABIERTA, recibidos.get(0).estado());
        assertEquals(EstadoTapa.CERRADA, recibidos.get(1).estado());
        assertEquals(2, sensor.transiciones());
    }

    @Test
    @DisplayName("No publica evento si el estado no cambia: sólo hay transiciones")
    void noPublicaEventoSinTransicion() {
        SensorPosicionTapa sensor = new SensorPosicionTapa();
        List<EventoTapa> recibidos = new ArrayList<>();
        sensor.agregarEscucha(recibidos::add);

        sensor.cerrar();
        sensor.cerrar();
        sensor.abrir();
        sensor.abrir();

        assertEquals(1, recibidos.size());
        assertEquals(EstadoTapa.ABIERTA, recibidos.get(0).estado());
    }

    @Test
    @DisplayName("La lectura incoherente se publica marcada como tal (CU-03, flujo alternativo 5)")
    void lecturaIncoherenteSeMarcaComoIncoherente() {
        SensorPosicionTapa sensor = new SensorPosicionTapa();
        List<EventoTapa> recibidos = new ArrayList<>();
        sensor.agregarEscucha(recibidos::add);

        sensor.inyectarLecturaIncoherente(EstadoTapa.ABIERTA);

        assertEquals(1, recibidos.size());
        assertFalse(recibidos.get(0).esCoherente());
        assertEquals(EstadoTapa.ABIERTA, recibidos.get(0).estado());
        // El estado físico no cambió: la falla es de la lectura, no de la tapa.
        assertEquals(EstadoTapa.CERRADA, sensor.estado());
        assertEquals(0, sensor.transiciones());
    }

    @Test
    @DisplayName("Notifica a todos los objetos límite conectados")
    void notificaATodosLosEscuchas() {
        SensorPosicionTapa sensor = new SensorPosicionTapa();
        List<EventoTapa> primero = new ArrayList<>();
        List<EventoTapa> segundo = new ArrayList<>();
        sensor.agregarEscucha(primero::add);
        sensor.agregarEscucha(segundo::add);

        sensor.abrir();

        assertEquals(1, primero.size());
        assertEquals(1, segundo.size());
        assertEquals(primero.get(0), segundo.get(0));
    }

    @Test
    @DisplayName("Un objeto límite dado de baja deja de recibir eventos")
    void quitarEscuchaDejaDeNotificar() {
        SensorPosicionTapa sensor = new SensorPosicionTapa();
        List<EventoTapa> recibidos = new ArrayList<>();
        ar.edu.unse.sistemas3.eventos.EscuchaTapa escucha = recibidos::add;
        sensor.agregarEscucha(escucha);
        sensor.abrir();

        sensor.quitarEscucha(escucha);

        sensor.cerrar();
        assertEquals(1, recibidos.size());
    }

    @Test
    @DisplayName("La detección respeta el retardo de conmutación del sensor")
    void respetaElRetardoDeDeteccion() {
        SensorPosicionTapa sensor = new SensorPosicionTapa();

        long inicio = System.nanoTime();
        sensor.abrir();
        long transcurrido = Duration.ofNanos(System.nanoTime() - inicio).toMillis();

        assertTrue(transcurrido >= SensorPosicionTapa.RETARDO_DETECCION_MS,
                "el sensor debería tardar al menos " + SensorPosicionTapa.RETARDO_DETECCION_MS + " ms");
    }
}
