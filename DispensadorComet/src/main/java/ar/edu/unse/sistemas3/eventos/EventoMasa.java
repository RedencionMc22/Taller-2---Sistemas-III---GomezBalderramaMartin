package ar.edu.unse.sistemas3.eventos;

import java.time.Instant;

/**
 * Lectura notificada por la balanza de precisión.
 *
 * <p>El proyecto COMET define para {@code BalanzaDePrecision} el evento
 * «masa detectada» junto con la medida de masa del recipiente. Un mismo evento
 * transporta la medida y el indicador de detección para que el objeto límite no
 * tenga que consultar el hardware dos veces.</p>
 */
public final class EventoMasa {

    private final double masaGramos;
    private final Instant instante;
    private final boolean detectaMasa;

    public EventoMasa(double masaGramos, Instant instante, boolean detectaMasa) {
        this.masaGramos = masaGramos;
        this.instante = instante;
        this.detectaMasa = detectaMasa;
    }

    /** Masa neta del recipiente, en gramos. */
    public double masaGramos() {
        return masaGramos;
    }

    public Instant instante() {
        return instante;
    }

    /** @return {@code true} si la masa alcanza el umbral de detección de la balanza. */
    public boolean detectaMasa() {
        return detectaMasa;
    }

    @Override
    public String toString() {
        return "EventoMasa[" + masaGramos + " g, " + (detectaMasa ? "masa detectada" : "sin masa") + "]";
    }
}
