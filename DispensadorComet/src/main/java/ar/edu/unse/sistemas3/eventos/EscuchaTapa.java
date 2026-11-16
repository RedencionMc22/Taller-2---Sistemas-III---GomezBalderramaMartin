package ar.edu.unse.sistemas3.eventos;

/**
 * Contrato de notificación del sensor de tapa.
 *
 * <p>Lo implementan tanto el objeto límite {@code SensorPosicionTapaInput}
 * (que escucha al dispositivo) como los objetos de control que en la etapa
 * siguiente consumirán el evento.</p>
 */
@FunctionalInterface
public interface EscuchaTapa {

    void tapaCambio(EventoTapa evento);
}
