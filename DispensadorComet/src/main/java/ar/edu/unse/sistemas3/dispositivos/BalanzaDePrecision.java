package ar.edu.unse.sistemas3.dispositivos;

import ar.edu.unse.sistemas3.eventos.EscuchaMasa;
import ar.edu.unse.sistemas3.eventos.EventoMasa;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simulación del dispositivo físico <b>BalanzaDePrecision</b>.
 *
 * <p>{@code BalanzaDePrecisionIO}. Mide la masa del recipiente de retiro: habilita
 * el desbloqueo de la tapa, verifica el retiro al cerrar y cuantifica la masa
 * remanente antes del descarte.</p>
 *
 * <p>Entradas que entrega al software: evento «masa detectada» y medida de masa.
 * Salidas que recibe del software: comandos de tara y de lectura de masa.</p>
 */
public final class BalanzaDePrecision {

    /** Umbral a partir del cual la balanza informa «masa detectada», en gramos. */
    public static final double UMBRAL_MASA_G = 1.0;

    /** Tiempo de estabilización de la lectura, en milisegundos. */
    public static final long TIEMPO_ESTABILIZACION_MS = 40L;

    private final List<EscuchaMasa> escuchas = new CopyOnWriteArrayList<>();

    private double masaGramos;
    private long lecturas;
    private long taras;

    /**
     * Deposita masa en el recipiente (equivale a que caiga la dosis o a que el paciente deje pastillas).
     * @param gramos masa neta depositada, no negativa
     */
    public void colocarMasa(double gramos) {
        if (gramos < 0) {
            throw new IllegalArgumentException("La masa no puede ser negativa: " + gramos);
        }
        cambiarMasa(gramos);
    }

    /** El recipiente queda vacío: el paciente retiró la dosis. */
    public void retirarMasa() {
        cambiarMasa(0);
    }

    private void cambiarMasa(double nuevaMasa) {
        esperar(TIEMPO_ESTABILIZACION_MS);
        EventoMasa evento;
        synchronized (this) {
            masaGramos = nuevaMasa;
            evento = new EventoMasa(nuevaMasa, Instant.now(), nuevaMasa >= UMBRAL_MASA_G);
        }
        publicar(evento);
    }

    /** Comando de lectura de masa: devuelve la masa neta actual, en gramos. */
    public synchronized double leerMasa() {
        lecturas++;
        return masaGramos;
    }

    /** Comando de tara: deja la balanza en cero con el recipiente vacío. */
    public void tarar() {
        synchronized (this) {
            masaGramos = 0;
            taras++;
        }
        publicar(new EventoMasa(0, Instant.now(), false));
    }

    public synchronized boolean detectaMasa() {
        return masaGramos >= UMBRAL_MASA_G;
    }

    public synchronized long lecturas() {
        return lecturas;
    }

    public synchronized long taras() {
        return taras;
    }

    private void publicar(EventoMasa evento) {
        for (EscuchaMasa escucha : escuchas) {
            escucha.masaCambio(evento);
        }
    }

    public void agregarEscucha(EscuchaMasa escucha) {
        escuchas.add(escucha);
    }

    public void quitarEscucha(EscuchaMasa escucha) {
        escuchas.remove(escucha);
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
