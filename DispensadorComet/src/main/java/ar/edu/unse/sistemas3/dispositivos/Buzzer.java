package ar.edu.unse.sistemas3.dispositivos;

/**
 * Simulación del dispositivo físico <b>Buzzer</b>.
 *
 * <p>{@code BuzzerOutput}.
 * Es la alarma sonora de aviso de toma; recibe los comandos de activar y
 * desactivar la alarma.</p>
 */
public final class Buzzer {

    /** Retardo de conmutación del zumbador, en milisegundos. */
    public static final long RETARDO_CONMUTACION_MS = 15L;

    private boolean sonando;
    private long conmutaciones;

    /** Comando «activar alarma sonora». */
    public void encender() {
        conmutar(true);
    }

    /** Comando «desactivar alarma sonora». */
    public void apagar() {
        conmutar(false);
    }

    private void conmutar(boolean nuevoEstado) {
        esperar(RETARDO_CONMUTACION_MS);
        synchronized (this) {
            if (sonando == nuevoEstado) {
                return; // ya estaba en ese estado: el comando no produce conmutación
            }
            sonando = nuevoEstado;
            conmutaciones++;
        }
    }

    public synchronized boolean estaSonando() {
        return sonando;
    }

    /** Cantidad de conmutaciones efectivas (encendidos y apagados). */
    public synchronized long conmutaciones() {
        return conmutaciones;
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
