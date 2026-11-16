package ar.edu.unse.sistemas3.bitacora;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Bitácora de sucesos del banco de pruebas.
 *
 * <p>Es el punto único de registro de todo lo que ocurre entre el mundo físico
 * simulado y los objetos límite. La interfaz gráfica se suscribe para mostrar
 * los sucesos en pantalla y las pruebas la usan como oráculo.</p>
 *
 * <p>La implementación es segura para uso concurrente: los dispositivos se
 * accionan desde el hilo de eventos de Swing ({@code EDT}) y las pruebas pueden
 * escribir desde varios hilos a la vez.</p>
 */
public final class RegistroEventos {

    /** Severidad de un suceso registrado. */
    public enum Nivel {
        INFO,
        ALERTA,
        FALLA
    }

    /**
     * Un suceso registrado: instante, severidad, origen (dispositivo u objeto
     * límite) y descripción.
     */
    public record Entrada(Instant instante, Nivel nivel, String origen, String mensaje) {

        private static final DateTimeFormatter HORA =
                DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

        public String horaFormateada() {
            return HORA.format(instante);
        }

        @Override
        public String toString() {
            return String.format("%s  %-6s  %-24s  %s", horaFormateada(), nivel, origen, mensaje);
        }
    }

    private final List<Entrada> entradas = new CopyOnWriteArrayList<>();
    private final List<Consumer<Entrada>> suscriptores = new CopyOnWriteArrayList<>();

    public void registrar(Nivel nivel, String origen, String mensaje) {
        Entrada entrada = new Entrada(Instant.now(), nivel, origen, mensaje);
        entradas.add(entrada);
        for (Consumer<Entrada> suscriptor : suscriptores) {
            suscriptor.accept(entrada);
        }
    }

    public void info(String origen, String mensaje) {
        registrar(Nivel.INFO, origen, mensaje);
    }

    public void alerta(String origen, String mensaje) {
        registrar(Nivel.ALERTA, origen, mensaje);
    }

    public void falla(String origen, String mensaje) {
        registrar(Nivel.FALLA, origen, mensaje);
    }

    /** Copia inmutable de los sucesos registrados, en orden cronológico. */
    public List<Entrada> entradas() {
        return List.copyOf(entradas);
    }

    public int cantidad() {
        return entradas.size();
    }

    public long cantidad(Nivel nivel) {
        return entradas.stream().filter(e -> e.nivel() == nivel).count();
    }

    public void suscribir(Consumer<Entrada> suscriptor) {
        suscriptores.add(suscriptor);
    }

    public void limpiar() {
        entradas.clear();
    }

    public String comoTexto() {
        StringBuilder sb = new StringBuilder();
        for (Entrada entrada : entradas) {
            sb.append(entrada).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
