package ar.edu.unse.sistemas3.dispositivos;

/**
 * Simulación del dispositivo físico <b>CerraduraTapa</b>.
 *
 * <p>{@code CerraduraTapaOutput}. Bloquea y desbloquea la tapa del recipiente
 * compartido; recibe los comandos de desbloquear (sólo con masa detectada) y de
 * bloquear.</p>
 *
 * <p>El dispositivo arranca bloqueado, que es el estado inicial seguro que fija
 * CU-10, paso 3. La regla «sólo con masa detectada» no se implementa acá:
 * es una regla del sistema de software y vive en el objeto límite
 * {@code CerraduraTapaOutput}, que consulta a la balanza antes de ordenar el
 * desbloqueo.</p>
 */
public final class CerraduraTapa {

    /** Retardo de actuación del cerrojo, en milisegundos. */
    public static final long RETARDO_ACTUACION_MS = 30L;

    private boolean bloqueada = true;
    private long bloqueos;
    private long desbloqueos;

    /** Comando «bloquear». */
    public void bloquear() {
        conmutar(true);
        synchronized (this) {
            bloqueos++;
        }
    }

    /** Comando «desbloquear». */
    public void desbloquear() {
        conmutar(false);
        synchronized (this) {
            desbloqueos++;
        }
    }

    private void conmutar(boolean nuevoEstado) {
        esperar(RETARDO_ACTUACION_MS);
        synchronized (this) {
            bloqueada = nuevoEstado;
        }
    }

    public synchronized boolean estaBloqueada() {
        return bloqueada;
    }

    public synchronized long bloqueos() {
        return bloqueos;
    }

    public synchronized long desbloqueos() {
        return desbloqueos;
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
