package ar.edu.unse.sistemas3.herramientas;

import ar.edu.unse.sistemas3.banco.BancoPruebasFrame;
import ar.edu.unse.sistemas3.limite.DesbloqueoNoPermitidoException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Genera las capturas de pantalla que se incluyen en el informe del taller.
 *
 * <p>No abre ventanas: construye el banco de pruebas, lo dispone con el mismo
 * tamaño con el que se usa en clase y lo dibuja sobre una imagen. Se ejecuta con
 * {@code python build.py capturas} o desde NetBeans como cualquier clase con
 * {@code main}.</p>
 */
public final class CapturaPantalla {

    private static final String CARPETA = "docs/capturas";

    private CapturaPantalla() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("sun.java2d.uiScale", "1");
        new File(CARPETA).mkdirs();

        final BancoPruebasFrame[] banco = new BancoPruebasFrame[1];

        SwingUtilities.invokeAndWait(() -> {
            banco[0] = new BancoPruebasFrame();
            banco[0].setSize(1120, 700);
            banco[0].addNotify();
            banco[0].validate();
        });

        capturar(banco[0], "01-banco-inicial.png");

        SwingUtilities.invokeAndWait(() -> {
            banco[0].balanza().colocarMasa(500);
            banco[0].buzzerOutput().activar();
            banco[0].sensorTapa().abrir();
            banco[0].buzzerOutput().desactivar();
            banco[0].actualizarVistas();
        });
        capturar(banco[0], "02-tapa-abierta.png");

        SwingUtilities.invokeAndWait(() -> {
            banco[0].sensorTapa().cerrar();
            banco[0].balanza().retirarMasa();
            banco[0].cerraduraOutput().bloquear();
            banco[0].actualizarVistas();
        });
        capturar(banco[0], "03-cierre-y-bloqueo.png");

        SwingUtilities.invokeAndWait(() -> {
            try {
                banco[0].cerraduraOutput().desbloquear();
            } catch (DesbloqueoNoPermitidoException e) {
                // La captura documenta el rechazo del interlock.
            }
            banco[0].sensorTapa().inyectarLecturaIncoherente(
                    ar.edu.unse.sistemas3.eventos.EstadoTapa.ABIERTA);
            banco[0].actualizarVistas();
        });
        capturar(banco[0], "04-interlock-y-lectura-incoherente.png");

        System.out.println("Capturas generadas en " + new File(CARPETA).getAbsolutePath());
        System.exit(0);
    }

    private static void capturar(BancoPruebasFrame banco, String archivo) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            banco.validate();
            banco.doLayout();
            Container contenido = banco.getContentPane();
            contenido.validate();
            int ancho = Math.max(contenido.getWidth(), 1120);
            int alto = Math.max(contenido.getHeight(), 660);
            BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = imagen.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            contenido.printAll(g2);
            g2.dispose();
            try {
                ImageIO.write(imagen, "png", new File(CARPETA, archivo));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo escribir " + archivo, e);
            }
        });
    }
}
