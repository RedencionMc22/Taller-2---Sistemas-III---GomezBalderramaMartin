package ar.edu.unse.sistemas3.limite;

import ar.edu.unse.sistemas3.bitacora.RegistroEventos;
import ar.edu.unse.sistemas3.dispositivos.CerraduraTapa;
import ar.edu.unse.sistemas3.util.Formato;

import java.util.Objects;

/**
 * Objeto límite «output» CerraduraTapaOutput (Tabla 1 del proyecto
 * COMET).
 *
 * <p>Ejecuta los comandos de bloqueo y desbloqueo de la tapa del recipiente
 * compartido. Antes de desbloquear verifica la precondición del sistema: tiene
 * que haber masa detectada en el recipiente. Si no la hay, rechaza el comando
 * con {@link DesbloqueoNoPermitidoException} y registra la falla.</p>
 *
 * <p>Cada comando devuelve su duración en milisegundos para poder verificar el
 * requisito de tiempo RT-06 («bloqueo de tapa en menos de 500 ms»).</p>
 */
public final class CerraduraTapaOutput {

    public static final String ORIGEN = "CerraduraTapaOutput";

    /** Plazo máximo de la operación de bloqueo/desbloqueo (RT-06), en milisegundos. */
    public static final long LIMITE_RT06_MS = 500L;

    private final CerraduraTapa dispositivo;
    private final BalanzaDePrecisionIO balanza;
    private final RegistroEventos registro;

    public CerraduraTapaOutput(CerraduraTapa dispositivo,
                               BalanzaDePrecisionIO balanza,
                               RegistroEventos registro) {
        this.dispositivo = Objects.requireNonNull(dispositivo, "dispositivo");
        this.balanza = Objects.requireNonNull(balanza, "balanza");
        this.registro = Objects.requireNonNull(registro, "registro");
    }

    /**
     * Comando «bloquear».
     *
     * @return duración del comando en milisegundos
     */
    public long bloquear() {
        long inicio = System.nanoTime();
        dispositivo.bloquear();
        long duracion = milisegundosDesde(inicio);
        registro.info(ORIGEN, "comando «bloquear» en " + Formato.milisegundos(duracion)
                + " (RT-06 ≤ " + LIMITE_RT06_MS + " ms)");
        if (duracion > LIMITE_RT06_MS) {
            registro.alerta(ORIGEN, "RT-06 incumplido: el bloqueo tardó "
                    + Formato.milisegundos(duracion));
        }
        return duracion;
    }

    /**
     * Comando «desbloquear», sólo si la balanza detecta masa.
     *
     * @return duración del comando en milisegundos
     * @throws DesbloqueoNoPermitidoException si el recipiente está vacío
     */
    public long desbloquear() throws DesbloqueoNoPermitidoException {
        if (!balanza.detectaMasa()) {
            registro.falla(ORIGEN, "desbloqueo rechazado: no hay masa detectada en el recipiente "
                    + "(Tabla 1, CerraduraTapa)");
            throw new DesbloqueoNoPermitidoException(
                    "No se puede desbloquear la tapa sin masa detectada en el recipiente");
        }
        long inicio = System.nanoTime();
        dispositivo.desbloquear();
        long duracion = milisegundosDesde(inicio);
        registro.info(ORIGEN, "comando «desbloquear» en " + Formato.milisegundos(duracion));
        return duracion;
    }

    public boolean estaBloqueada() {
        return dispositivo.estaBloqueada();
    }

    public long bloqueos() {
        return dispositivo.bloqueos();
    }

    public long desbloqueos() {
        return dispositivo.desbloqueos();
    }

    public String descripcionDispositivo() {
        return "CerraduraTapa («external output device»)";
    }

    private static long milisegundosDesde(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000L;
    }
}
