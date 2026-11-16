package ar.edu.unse.sistemas3.limite;

/**
 * Excepción que se lanza cuando se intenta desbloquear la tapa sin que la
 * balanza detecte masa en el recipiente.
 *
 * <p>Implementa la restricción que la Tabla 1 del proyecto COMET asigna a
 * {@code CerraduraTapa}: «comandos de desbloquear (sólo con masa detectada) y de
 * bloquear». Es una excepción síncrona detectada por la aplicación: el
 * objeto límite conoce la regla y la hace cumplir antes de accionar el cerrojo,
 * de modo que el sistema nunca queda con la tapa desbloqueada y el recipiente
 * vacío.</p>
 */
public class DesbloqueoNoPermitidoException extends Exception {

    private static final long serialVersionUID = 1L;

    public DesbloqueoNoPermitidoException(String mensaje) {
        super(mensaje);
    }
}
