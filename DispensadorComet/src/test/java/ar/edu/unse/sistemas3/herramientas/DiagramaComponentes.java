package ar.edu.unse.sistemas3.herramientas;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Dibuja el diagrama de componentes de las actividades 1 y 2.
 *
 * <p>Se ejecuta una vez para generar la imagen que se inserta en el informe; no
 * forma parte del sistema bajo estudio. El lienzo se dimensiona para que, al
 * imprimirse a 16,6 cm de ancho, ningún texto quede por debajo de los 7 puntos.</p>
 */
public final class DiagramaComponentes {

    private static final int ANCHO = 1000;
    private static final int ALTO = 704;

    private static final Color TINTA = new Color(0x22, 0x2A, 0x35);
    private static final Color TENUE = new Color(0x55, 0x62, 0x72);
    private static final Color ENTRADA = new Color(0x15, 0x65, 0xC0);
    private static final Color SALIDA = new Color(0x6A, 0x1B, 0x9A);
    private static final Color FRANJA = new Color(0xF4, 0xF6, 0xFA);
    private static final Color BORDE_FRANJA = new Color(0xE0, 0xE6, 0xEE);

    private static final int ANCHO_CAJA = 320;
    private static final int ALTO_CAJA = 78;
    private static final int ALTO_FILA = 116;
    private static final int X_FISICO = 55;
    private static final int X_LIMITE = 625;
    private static final int Y_FILAS = 176;

    private static final int Y_FRANJA = 112;
    private static final int ALTO_FRANJA = 508;
    private static final int X_FRANJA_FISICO = X_FISICO - 18;
    private static final int X_FRANJA_LIMITE = X_LIMITE - 18;
    private static final int ANCHO_FRANJA = ANCHO_CAJA + 36;

    private DiagramaComponentes() {
    }

    public static void main(String[] args) throws Exception {
        BufferedImage imagen = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, ANCHO, ALTO);

        g.setColor(TINTA);
        g.setFont(new Font("Segoe UI", Font.BOLD, 24));
        g.drawString("Dispositivos simulados y objetos límite del subsistema «Retiro y evidencia»", 37, 46);
        g.setColor(TENUE);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        g.drawString("Taller 2 · Actividades 1 y 2 · Proyecto COMET — Dispensador Autónomo de Medicamentos", 37, 74);

        franja(g, X_FRANJA_FISICO, "MUNDO FÍSICO (simulado)", "hardware que se reemplaza en el sistema real");
        franja(g, X_FRANJA_LIMITE, "OBJETOS LÍMITE (software)", "«input» / «output» del modelo COMET");

        caja(g, X_FISICO, fila(0), "SensorPosicionTapa", "«external input device»", ENTRADA);
        caja(g, X_LIMITE, fila(0), "SensorPosicionTapaInput", "«input»", ENTRADA);

        caja(g, X_FISICO, fila(1), "BalanzaDePrecision", "«external input/output device»", ENTRADA);
        caja(g, X_LIMITE, fila(1), "BalanzaDePrecisionIO", "«input/output»", ENTRADA);

        caja(g, X_FISICO, fila(2), "Buzzer", "«external output device»", SALIDA);
        caja(g, X_LIMITE, fila(2), "BuzzerOutput", "«output»", SALIDA);

        caja(g, X_FISICO, fila(3), "CerraduraTapa", "«external output device»", SALIDA);
        caja(g, X_LIMITE, fila(3), "CerraduraTapaOutput", "«output»", SALIDA);

        int desde = X_FISICO + ANCHO_CAJA;
        int hasta = X_LIMITE;

        // Fila 1: la tapa informa transiciones al objeto límite (entrada)
        flecha(g, desde, centro(fila(0)), hasta, centro(fila(0)), true, false, ENTRADA);
        etiqueta(g, new String[]{"evento «tapa abierta»", "«tapa cerrada»"}, (desde + hasta) / 2, centro(fila(0)));

        // Fila 2: la balanza entrega la medida y recibe comandos (entrada/salida)
        flecha(g, desde, centro(fila(1)), hasta, centro(fila(1)), true, true, ENTRADA);
        etiqueta(g, new String[]{"«masa detectada»", "tara · lectura"}, (desde + hasta) / 2, centro(fila(1)));

        // Fila 3: el objeto límite acciona el zumbador (salida)
        flecha(g, hasta, centro(fila(2)), desde, centro(fila(2)), true, false, SALIDA);
        etiqueta(g, new String[]{"activar / desactivar", "alarma sonora"}, (desde + hasta) / 2, centro(fila(2)));

        // Fila 4: el objeto límite acciona la cerradura (salida)
        flecha(g, hasta, centro(fila(3)), desde, centro(fila(3)), true, false, SALIDA);
        etiqueta(g, new String[]{"bloquear / desbloquear"}, (desde + hasta) / 2, centro(fila(3)));

        nota(g);

        g.dispose();
        new File("docs").mkdirs();
        File salida = new File("docs/diagrama-componentes.png");
        ImageIO.write(imagen, "png", salida);
        System.out.println("Diagrama generado en " + salida.getAbsolutePath());
    }

    private static int fila(int indice) {
        return Y_FILAS + indice * ALTO_FILA;
    }

    private static int centro(int yCaja) {
        return yCaja + ALTO_CAJA / 2;
    }

    private static void franja(Graphics2D g, int x, String titulo, String detalle) {
        g.setColor(FRANJA);
        g.fillRoundRect(x, Y_FRANJA, ANCHO_FRANJA, ALTO_FRANJA, 16, 16);
        g.setColor(BORDE_FRANJA);
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, Y_FRANJA, ANCHO_FRANJA, ALTO_FRANJA, 16, 16);

        g.setColor(TENUE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 15));
        centrar(g, titulo, x, Y_FRANJA + 30, ANCHO_FRANJA);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        centrar(g, detalle, x, Y_FRANJA + 51, ANCHO_FRANJA);
    }

    private static void caja(Graphics2D g, int x, int y, String nombre, String estereotipo, Color acento) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, ANCHO_CAJA, ALTO_CAJA, 12, 12);
        g.setColor(acento);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, ANCHO_CAJA, ALTO_CAJA, 12, 12);

        g.setColor(TINTA);
        g.setFont(new Font("Segoe UI", Font.BOLD, 20));
        centrar(g, nombre, x, y + 33, ANCHO_CAJA);
        g.setColor(TENUE);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        centrar(g, estereotipo, x, y + 58, ANCHO_CAJA);
    }

    private static void flecha(Graphics2D g, int x1, int y1, int x2, int y2,
                               boolean solida, boolean doble, Color color) {
        g.setColor(color);
        g.setStroke(solida ? new BasicStroke(2f)
                : new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                new float[]{6f, 5f}, 0f));
        g.drawLine(x1, y1, x2, y2);
        punta(g, x1, y1, x2, y2, color);
        if (doble) {
            punta(g, x2, y2, x1, y1, color);
        }
    }

    private static void punta(Graphics2D g, int xDesde, int yDesde, int xHasta, int yHasta, Color color) {
        double angulo = Math.atan2(yHasta - yDesde, xHasta - xDesde);
        int largo = 12;
        int ancho = 5;
        Path2D punta = new Path2D.Double();
        punta.moveTo(xHasta, yHasta);
        punta.lineTo(xHasta - largo * Math.cos(angulo) + ancho * Math.sin(angulo),
                yHasta - largo * Math.sin(angulo) - ancho * Math.cos(angulo));
        punta.lineTo(xHasta - largo * Math.cos(angulo) - ancho * Math.sin(angulo),
                yHasta - largo * Math.sin(angulo) + ancho * Math.cos(angulo));
        punta.closePath();
        g.setColor(color);
        g.fill(punta);
    }

    /** Etiqueta en una o dos líneas, centrada sobre la flecha, con fondo blanco. */
    private static void etiqueta(Graphics2D g, String[] lineas, int centroX, int centroY) {
        g.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        FontMetrics fm = g.getFontMetrics();
        int anchoMaximo = 0;
        for (String linea : lineas) {
            anchoMaximo = Math.max(anchoMaximo, fm.stringWidth(linea));
        }
        int alto = lineas.length * 20;
        g.setColor(Color.WHITE);
        g.fillRoundRect(centroX - anchoMaximo / 2 - 9, centroY - alto / 2 - 4,
                anchoMaximo + 18, alto + 8, 12, 12);
        g.setColor(TENUE);
        int y = centroY - alto / 2 + 15;
        for (String linea : lineas) {
            centrar(g, linea, centroX - anchoMaximo / 2, y, anchoMaximo);
            y += 20;
        }
    }

    /** Aclaración sobre los objetos de control, que son materia de la actividad 3. */
    private static void nota(Graphics2D g) {
        g.setColor(new Color(0xF8, 0xFA, 0xFC));
        g.fillRoundRect(X_FRANJA_FISICO, 636, ANCHO - 2 * X_FRANJA_FISICO, 48, 10, 10);
        g.setColor(TENUE);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g.drawString("Los objetos de control (ControlRetiro y ControlGrabacion) consumirán estos eventos y emitirán", 55, 658);
        g.drawString("estos comandos; se implementan en la actividad 3 y no forman parte de esta entrega.", 55, 678);
    }

    private static void centrar(Graphics2D g, String texto, int x, int y, int ancho) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, x + (ancho - fm.stringWidth(texto)) / 2, y);
    }
}
