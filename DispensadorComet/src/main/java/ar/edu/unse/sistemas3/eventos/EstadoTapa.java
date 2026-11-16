package ar.edu.unse.sistemas3.eventos;

/**
 * Posición de la tapa transparente del recipiente de retiro.
 *
 * <p>Corresponde a los dos eventos que el proyecto COMET asigna al dispositivo
 * {@code SensorPosicionTapa} (Tabla 1): «tapa abierta» y «tapa cerrada».</p>
 */
public enum EstadoTapa {

    ABIERTA("tapa abierta"),
    CERRADA("tapa cerrada");

    private final String descripcion;

    EstadoTapa(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
