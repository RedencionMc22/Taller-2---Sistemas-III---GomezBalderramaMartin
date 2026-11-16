package ar.edu.unse.sistemas3.limite;

import ar.edu.unse.sistemas3.bitacora.RegistroEventos;
import ar.edu.unse.sistemas3.dispositivos.Buzzer;

import java.util.Objects;

/**
 * Objeto límite «output» BuzzerOutput (Tabla 1 del proyecto COMET).
 *
 * <p>Traduce los comandos del sistema en acciones sobre la alarma sonora.
 * El control nunca enciende el buzzer directamente: lo hace a través de este
 * objeto, que además deja el comando en la bitácora.</p>
 */
public final class BuzzerOutput {

    public static final String ORIGEN = "BuzzerOutput";

    private final Buzzer dispositivo;
    private final RegistroEventos registro;

    public BuzzerOutput(Buzzer dispositivo, RegistroEventos registro) {
        this.dispositivo = Objects.requireNonNull(dispositivo, "dispositivo");
        this.registro = Objects.requireNonNull(registro, "registro");
    }

    /** Comando «activar alarma sonora». */
    public void activar() {
        dispositivo.encender();
        registro.info(ORIGEN, "comando «activar alarma sonora»");
    }

    /** Comando «desactivar alarma sonora». */
    public void desactivar() {
        dispositivo.apagar();
        registro.info(ORIGEN, "comando «desactivar alarma sonora»");
    }

    public boolean estaActivo() {
        return dispositivo.estaSonando();
    }

    public long conmutaciones() {
        return dispositivo.conmutaciones();
    }

    public String descripcionDispositivo() {
        return "Buzzer («external output device»)";
    }
}
