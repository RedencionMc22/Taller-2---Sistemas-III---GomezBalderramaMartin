package ar.edu.unse.sistemas3.eventos;

/**
 * Contrato de notificación de la balanza de precisión.
 */
@FunctionalInterface
public interface EscuchaMasa {

    void masaCambio(EventoMasa evento);
}
