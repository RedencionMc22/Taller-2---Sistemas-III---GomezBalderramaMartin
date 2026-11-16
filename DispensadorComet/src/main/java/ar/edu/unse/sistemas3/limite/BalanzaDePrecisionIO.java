package ar.edu.unse.sistemas3.limite;

import ar.edu.unse.sistemas3.bitacora.RegistroEventos;
import ar.edu.unse.sistemas3.dispositivos.BalanzaDePrecision;
import ar.edu.unse.sistemas3.eventos.EscuchaMasa;
import ar.edu.unse.sistemas3.eventos.EventoMasa;
import ar.edu.unse.sistemas3.util.Formato;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Objeto límite <b>«input/output» BalanzaDePrecisionIO</b> (Tabla 1 del
 * proyecto COMET).
 *
 * <p>Provee la interfaz de software de la balanza:</p>
 * <ul>
 *   <li>Entradas al sistema: evento «masa detectada» y la medida de masa
 *       del recipiente.</li>
 *   <li>Salidas del sistema: comandos de tara y de lectura de masa.</li>
 * </ul>
 *
 * <p>El evento «masa detectada» sólo se informa a los objetos de control cuando
 * cambia la condición de detección, no en cada variación de masa. Así, dejar
 * caer la dosis produce un único evento aunque la balanza se estabilice en
 * varios pasos.</p>
 */
public final class BalanzaDePrecisionIO implements EscuchaMasa {

    public static final String ORIGEN = "BalanzaDePrecisionIO";

    private final BalanzaDePrecision dispositivo;
    private final RegistroEventos registro;
    private final List<EscuchaMasa> escuchas = new CopyOnWriteArrayList<>();

    private volatile EventoMasa ultimoEvento;

    public BalanzaDePrecisionIO(BalanzaDePrecision dispositivo, RegistroEventos registro) {
        this.dispositivo = Objects.requireNonNull(dispositivo, "dispositivo");
        this.registro = Objects.requireNonNull(registro, "registro");
        this.dispositivo.agregarEscucha(this);
    }

    @Override
    public void masaCambio(EventoMasa evento) {
        boolean antesDetectaba = ultimoEvento != null && ultimoEvento.detectaMasa();
        ultimoEvento = evento;

        if (evento.detectaMasa() != antesDetectaba) {
            if (evento.detectaMasa()) {
                registro.info(ORIGEN, "evento «masa detectada» (" + Formato.masa(evento.masaGramos()) + ")");
            } else {
                registro.info(ORIGEN, "recipiente sin masa");
            }
            for (EscuchaMasa escucha : escuchas) {
                escucha.masaCambio(evento);
            }
        }
    }

    /** Comando de lectura de masa (salida del sistema hacia el dispositivo). */
    public double leerMasa() {
        double masa = dispositivo.leerMasa();
        registro.info(ORIGEN, "comando «leer masa» → " + Formato.masa(masa));
        return masa;
    }

    /** Comando de tara (salida del sistema hacia el dispositivo). */
    public void tarar() {
        dispositivo.tarar();
        registro.info(ORIGEN, "comando «tara»: balanza en cero");
    }

    public boolean detectaMasa() {
        return dispositivo.detectaMasa();
    }

    /**
     * Lectura de masa para las vistas del banco de pruebas.
     *
     * <p>No registra el comando en la bitácora: a diferencia de
     * {@link #leerMasa()}, no representa una salida del sistema hacia el
     * dispositivo sino la lectura del instrumento que muestra la pantalla.</p>
     *
     * @return masa neta actual, en gramos
     */
    public double masaActual() {
        return dispositivo.leerMasa();
    }

    public EventoMasa ultimoEvento() {
        return ultimoEvento;
    }

    public void agregarEscucha(EscuchaMasa escucha) {
        escuchas.add(escucha);
    }

    public void quitarEscucha(EscuchaMasa escucha) {
        escuchas.remove(escucha);
    }

    public String descripcionDispositivo() {
        return "BalanzaDePrecision («external input/output device»)";
    }
}
