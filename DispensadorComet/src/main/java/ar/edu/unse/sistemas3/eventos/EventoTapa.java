package ar.edu.unse.sistemas3.eventos;

import java.time.Instant;
import java.util.Objects;

/**
 * Evento «tapa abierta» / «tapa cerrada» generado por el dispositivo
 * {@code SensorPosicionTapa} y entregado al objeto límite
 * {@code SensorPosicionTapaInput}.
 *
 * <p>El atributo {@link #esCoherente()} permite distinguir una transición real
 * del sensor de una lectura espuria (interferencia o rebote de contacto). El
 * caso de uso CU-03, flujo alternativo 5, exige que esa lectura se trate como
 * falla y se registre.</p>
 */
public final class EventoTapa {

    private final EstadoTapa estado;
    private final Instant instante;
    private final boolean coherente;

    public EventoTapa(EstadoTapa estado, Instant instante, boolean coherente) {
        this.estado = Objects.requireNonNull(estado, "estado");
        this.instante = Objects.requireNonNull(instante, "instante");
        this.coherente = coherente;
    }

    public EstadoTapa estado() {
        return estado;
    }

    public Instant instante() {
        return instante;
    }

    /**
     * @return {@code true} si el evento corresponde a una transición física real
     *         de la tapa; {@code false} si es una lectura incoherente inyectada.
     */
    public boolean esCoherente() {
        return coherente;
    }

    @Override
    public String toString() {
        return "EventoTapa[" + estado + ", " + instante + (coherente ? "" : ", INCOHERENTE") + "]";
    }
}
