package ar.edu.unse.sistemas3.banco;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

/**
 * Punto de entrada del banco de pruebas.
 *
 * <p>Se ejecuta desde NetBeans con <em>Run Project</em> (F6) o desde la línea de
 * comandos con {@code java -cp target/classes ar.edu.unse.sistemas3.banco.BancoPruebas}.</p>
 */
public final class BancoPruebas {

    private BancoPruebas() {
    }

    public static void main(String[] args) {
        configurarAspecto();
        SwingUtilities.invokeLater(() -> new BancoPruebasFrame().setVisible(true));
    }

    private static void configurarAspecto() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 12));
            UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 12));
        } catch (Exception e) {
            // Se continúa con el aspecto por defecto de la JVM.
        }
    }
}
