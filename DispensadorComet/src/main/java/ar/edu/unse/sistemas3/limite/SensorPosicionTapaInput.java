package ar.edu.unse.sistemas3.limite;

import ar.edu.unse.sistemas3.bitacora.RegistroEventos;
import ar.edu.unse.sistemas3.dispositivos.SensorPosicionTapa;
import ar.edu.unse.sistemas3.eventos.EscuchaTapa;
import ar.edu.unse.sistemas3.eventos.EstadoTapa;
import ar.edu.unse.sistemas3.eventos.EventoTapa;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Objeto límite «input» SensorPosicionTapaInput (Tabla 1 del proyecto
 * COMET).
 *
 * <p>Es el único objeto de software que habla con el dispositivo
 * {@link SensorPosicionTapa}. Recibe el evento «tapa abierta» o «tapa cerrada»,
 * lo registra con su instante y lo re-publica a los objetos de control. De este
 * modo el controlador no depende del hardware: en el taller se conecta a este
 * simulador y en el sistema real se conectaría al GPIO del sensor.</p>
 *
 * <p>Cuando el sensor entrega una lectura incoherente (CU-03, flujo alternativo
 * 5) el objeto límite no la descarta: la propaga marcada y la registra como
 * alerta, para que el control decida tratarla como falla.</p>
 */
public final class SensorPosicionTapaInput implements EscuchaTapa {

    public static final String ORIGEN = "SensorPosicionTapaInput";

    private final SensorPosicionTapa dispositivo;
    private final RegistroEventos registro;
    private final List<EscuchaTapa> escuchas = new CopyOnWriteArrayList<>();

    private volatile EventoTapa ultimoEvento;

    public SensorPosicionTapaInput(SensorPosicionTapa dispositivo, RegistroEventos registro) {
        this.dispositivo = Objects.requireNonNull(dispositivo, "dispositivo");
        this.registro = Objects.requireNonNull(registro, "registro");
        this.dispositivo.agregarEscucha(this);
    }

    @Override
    public void tapaCambio(EventoTapa evento) {
        ultimoEvento = evento;
        if (evento.esCoherente()) {
            registro.info(ORIGEN, "evento «" + evento.estado().descripcion() + "»");
        } else {
            registro.alerta(ORIGEN, "lectura incoherente: «" + evento.estado().descripcion()
                    + "» sin transición física previa (CU-03, flujo alternativo 5)");
        }
        for (EscuchaTapa escucha : escuchas) {
            escucha.tapaCambio(evento);
        }
    }

    public void agregarEscucha(EscuchaTapa escucha) {
        escuchas.add(escucha);
    }

    public void quitarEscucha(EscuchaTapa escucha) {
        escuchas.remove(escucha);
    }

    /** Último estado informado por el sensor, según el objeto límite. */
    public EstadoTapa estadoActual() {
        return dispositivo.estado();
    }

    public EventoTapa ultimoEvento() {
        return ultimoEvento;
    }

    public String descripcionDispositivo() {
        return "SensorPosicionTapa («external input device»)";
    }
}
