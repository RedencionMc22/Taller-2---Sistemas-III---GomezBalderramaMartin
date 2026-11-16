package ar.edu.unse.sistemas3.dispositivos;

import ar.edu.unse.sistemas3.eventos.EscuchaTapa;
import ar.edu.unse.sistemas3.eventos.EstadoTapa;
import ar.edu.unse.sistemas3.eventos.EventoTapa;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simulación del dispositivo físico <b>SensorPosicionTapa</b>.
 *
 * <p>{@code SensorPosicionTapaInput}. Detecta la apertura y el cierre de la tapa
 * del recipiente y registra cada transición.</p>
 *
 * <p>Esta clase representa el hardware: no conoce el sistema de
 * software, sólo publica los eventos «tapa abierta» y «tapa cerrada» a quien
 * esté conectado (el objeto límite). La interfaz gráfica del banco de pruebas
 * acciona los métodos {@link #abrir()} y {@link #cerrar()} en lugar de que una
 * persona levante la tapa.</p>
 *
 * <p>El retardo {@link #RETARDO_DETECCION_MS} modela el tiempo de respuesta
 * mecánico del sensor. No es un plazo del sistema: RT-10 y RT-06 se miden sobre
 * los objetos límite.</p>
 */
public final class SensorPosicionTapa {

    /** Retardo de conmutación del contacto del sensor, en milisegundos. */
    public static final long RETARDO_DETECCION_MS = 20L;

    private final List<EscuchaTapa> escuchas = new CopyOnWriteArrayList<>();

    private EstadoTapa estadoFisico = EstadoTapa.CERRADA;
    private long transiciones;

    /** El paciente levanta la tapa. */
    public void abrir() {
        conmutar(EstadoTapa.ABIERTA);
    }

    /** El paciente baja la tapa. */
    public void cerrar() {
        conmutar(EstadoTapa.CERRADA);
    }

    /**
     * Inyecta una lectura del sensor que no corresponde a ninguna transición
     * física, por ejemplo por interferencia o rebote del contacto.
     *
     * <p>Modela el flujo alternativo 5 de CU-03: «tapa abierta sin evento previo
     * de apertura». El evento se publica marcado como incoherente para que el
     * objeto límite lo registre como falla.</p>
     *
     * @param informada estado que el sensor informa erróneamente
     */
    public void inyectarLecturaIncoherente(EstadoTapa informada) {
        publicar(new EventoTapa(informada, Instant.now(), false));
    }

    private void conmutar(EstadoTapa nuevo) {
        esperar(RETARDO_DETECCION_MS);
        EventoTapa evento;
        synchronized (this) {
            if (estadoFisico == nuevo) {
                return; // la tapa ya estaba en ese estado: no hay transición que informar
            }
            estadoFisico = nuevo;
            transiciones++;
            evento = new EventoTapa(nuevo, Instant.now(), true);
        }
        publicar(evento);
    }

    private void publicar(EventoTapa evento) {
        for (EscuchaTapa escucha : escuchas) {
            escucha.tapaCambio(evento);
        }
    }

    public synchronized EstadoTapa estado() {
        return estadoFisico;
    }

    /** Cantidad de transiciones físicas registradas por el sensor. */
    public synchronized long transiciones() {
        return transiciones;
    }

    public void agregarEscucha(EscuchaTapa escucha) {
        escuchas.add(escucha);
    }

    public void quitarEscucha(EscuchaTapa escucha) {
        escuchas.remove(escucha);
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
