package ar.edu.unse.sistemas3.util;

import java.util.Locale;

/**
 * Formato numérico uniforme para registros, etiquetas de la interfaz y pruebas.
 *
 * <p>Se fija la convención argentina (coma decimal) para que el texto que se
 * muestra y el que se documenta coincidan, sin depender de la configuración
 * regional de la máquina donde se ejecute.</p>
 */
public final class Formato {

    private Formato() {
    }

    public static String masa(double gramos) {
        return String.format(Locale.US, "%.1f", gramos).replace('.', ',') + " g";
    }

    public static String milisegundos(long ms) {
        return ms + " ms";
    }
}
