package ar.edu.unse.sistemas3.banco;

import ar.edu.unse.sistemas3.bitacora.RegistroEventos;
import ar.edu.unse.sistemas3.dispositivos.BalanzaDePrecision;
import ar.edu.unse.sistemas3.dispositivos.Buzzer;
import ar.edu.unse.sistemas3.dispositivos.CerraduraTapa;
import ar.edu.unse.sistemas3.dispositivos.SensorPosicionTapa;
import ar.edu.unse.sistemas3.eventos.EstadoTapa;
import ar.edu.unse.sistemas3.limite.BalanzaDePrecisionIO;
import ar.edu.unse.sistemas3.limite.BuzzerOutput;
import ar.edu.unse.sistemas3.limite.CerraduraTapaOutput;
import ar.edu.unse.sistemas3.limite.DesbloqueoNoPermitidoException;
import ar.edu.unse.sistemas3.limite.SensorPosicionTapaInput;
import ar.edu.unse.sistemas3.util.Formato;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;

/**
 * Banco de pruebas de las actividades 1 y 2 del Taller 2.
 *
 * <p>La ventana muestra, a la izquierda, el <b>mundo físico simulado</b>
 * (sensor de tapa y balanza de precisión, que se accionan directamente) y, a la
 * derecha, los <b>objetos límite</b> que el software usa para comandar los
 * dispositivos de salida (buzzer y cerradura). Abajo se registra cada suceso con
 * su instante, su origen y su severidad.</p>
 *
 * <p>La ventana no contiene lógica de control: enlazar los eventos de entrada con
 * los comandos de salida es la actividad 3 (código de operación del controlador).
 * Acá se prueba únicamente la interacción entre dispositivos simulados y objetos
 * de interfaz.</p>
 */
@SuppressWarnings("serial")
public final class BancoPruebasFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color FONDO = new Color(0xF2, 0xF4, 0xF7);
    private static final Color TINTA = new Color(0x22, 0x2A, 0x35);
    private static final Color TENUE = new Color(0x6B, 0x76, 0x85);
    private static final Color ENTRADA = new Color(0x15, 0x65, 0xC0);
    private static final Color SALIDA = new Color(0x6A, 0x1B, 0x9A);
    private static final Color VERDE = new Color(0x2E, 0x7D, 0x32);
    private static final Color ROJO = new Color(0xC6, 0x28, 0x28);
    private static final Color AMBAR = new Color(0xF5, 0x9E, 0x0B);
    private static final Color GRIS = new Color(0x9E, 0x9E, 0x9E);

    private final transient RegistroEventos registro = new RegistroEventos();

    private final transient SensorPosicionTapa sensorTapa = new SensorPosicionTapa();
    private final transient BalanzaDePrecision balanza = new BalanzaDePrecision();
    private final transient Buzzer buzzer = new Buzzer();
    private final transient CerraduraTapa cerradura = new CerraduraTapa();

    private final transient SensorPosicionTapaInput sensorInput =
            new SensorPosicionTapaInput(sensorTapa, registro);
    private final transient BalanzaDePrecisionIO balanzaIO =
            new BalanzaDePrecisionIO(balanza, registro);
    private final transient BuzzerOutput buzzerOutput = new BuzzerOutput(buzzer, registro);
    private final transient CerraduraTapaOutput cerraduraOutput =
            new CerraduraTapaOutput(cerradura, balanzaIO, registro);

    private final IconoLuz luzTapa = new IconoLuz(14);
    private final IconoLuz luzBalanza = new IconoLuz(14);
    private final IconoLuz luzBuzzer = new IconoLuz(14);
    private final IconoLuz luzCerradura = new IconoLuz(14);

    private final JLabel lblTapa = new JLabel();
    private final JLabel lblTapaDetalle = new JLabel();
    private final JLabel lblMasa = new JLabel();
    private final JLabel lblBalanzaDetalle = new JLabel();
    private final JLabel lblBuzzer = new JLabel();
    private final JLabel lblBuzzerDetalle = new JLabel();
    private final JLabel lblCerradura = new JLabel();
    private final JLabel lblCerraduraDetalle = new JLabel();
    private final JLabel lblResumen = new JLabel();

    private final JTextArea areaRegistro = new JTextArea();
    private final JSpinner spMasa = new JSpinner(new SpinnerNumberModel(500.0, 0.0, 5000.0, 50.0));

    public BancoPruebasFrame() {
        super("Taller 2 · Actividades 1 y 2 · Dispositivos de E/S simulados");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(FONDO);

        add(construirEncabezado(), BorderLayout.NORTH);
        add(construirCentro(), BorderLayout.CENTER);
        add(construirRegistro(), BorderLayout.EAST);
        add(construirPie(), BorderLayout.SOUTH);

        registro.suscribir(entrada -> SwingUtilities.invokeLater(() -> {
            areaRegistro.append(entrada + System.lineSeparator());
            areaRegistro.setCaretPosition(areaRegistro.getDocument().getLength());
        }));

        registro.info("Banco de pruebas",
                "inicio: tapa cerrada y bloqueada (estado inicial seguro, CU-10 paso 3)");
        actualizarVistas();

        setMinimumSize(new Dimension(1040, 660));
        setSize(1100, 700);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Modelo expuesto para pruebas automatizadas y capturas de pantalla
    // ------------------------------------------------------------------

    public RegistroEventos registro() {
        return registro;
    }

    public SensorPosicionTapa sensorTapa() {
        return sensorTapa;
    }

    public BalanzaDePrecision balanza() {
        return balanza;
    }

    public Buzzer buzzer() {
        return buzzer;
    }

    public CerraduraTapa cerradura() {
        return cerradura;
    }

    public SensorPosicionTapaInput sensorInput() {
        return sensorInput;
    }

    public BalanzaDePrecisionIO balanzaIO() {
        return balanzaIO;
    }

    public BuzzerOutput buzzerOutput() {
        return buzzerOutput;
    }

    public CerraduraTapaOutput cerraduraOutput() {
        return cerraduraOutput;
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------

    private JPanel construirEncabezado() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(0x1F, 0x2B, 0x3A));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel titulo = new JLabel("Dispensador Autónomo de Medicamentos · Subsistema «Retiro y evidencia»");
        titulo.setFont(fuente(Font.BOLD, 18f));
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel("Taller 2 · Actividades 1 y 2 · Objetos de software que simulan dispositivos de E/S "
                + "y objetos límite que permiten interactuar con ellos");
        subtitulo.setFont(fuente(Font.PLAIN, 12f));
        subtitulo.setForeground(new Color(0xB6, 0xC1, 0xCD));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(subtitulo);

        panel.add(textos, BorderLayout.WEST);
        return panel;
    }

    private JPanel construirCentro() {
        JPanel grilla = new JPanel(new GridLayout(2, 2, 14, 14));
        grilla.setBackground(FONDO);
        grilla.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 7));

        grilla.add(tarjetaSensorTapa());
        grilla.add(tarjetaBalanza());
        grilla.add(tarjetaBuzzer());
        grilla.add(tarjetaCerradura());
        return grilla;
    }

    private JPanel tarjetaSensorTapa() {
        JButton abrir = boton("Abrir tapa");
        JButton cerrar = boton("Cerrar tapa");
        JButton incoherente = botonSecundario("Forzar lectura incoherente");

        abrir.addActionListener(e -> {
            sensorTapa.abrir();
            actualizarVistas();
        });
        cerrar.addActionListener(e -> {
            sensorTapa.cerrar();
            actualizarVistas();
        });
        incoherente.addActionListener(e -> {
            sensorTapa.inyectarLecturaIncoherente(EstadoTapa.ABIERTA);
            actualizarVistas();
        });

        JPanel contenido = columna(
                filaEstado(luzTapa, lblTapa),
                fila(abrir, cerrar),
                fila(incoherente),
                nota(lblTapaDetalle));

        lblTapaDetalle.setToolTipText("Cantidad de transiciones físicas y último evento recibido por el objeto límite");
        JPanel tarjeta = tarjeta("1 · Sensor de tapa", "SensorPosicionTapa · «external input device»",
                "ENTRADA", ENTRADA, sensorInput.descripcionDispositivo(), contenido);
        tarjeta.setToolTipText("El paciente abre o cierra la tapa: se acciona el hardware simulado");
        return tarjeta;
    }

    private JPanel tarjetaBalanza() {
        JButton colocar = boton("Colocar masa");
        JButton retirar = boton("Retirar masa");
        JButton tarar = botonSecundario("Tarar");
        JButton leer = botonSecundario("Leer masa");

        colocar.addActionListener(e -> {
            balanza.colocarMasa(valorMasa());
            actualizarVistas();
        });
        retirar.addActionListener(e -> {
            balanza.retirarMasa();
            actualizarVistas();
        });
        tarar.addActionListener(e -> {
            balanzaIO.tarar();
            actualizarVistas();
        });
        leer.addActionListener(e -> {
            balanzaIO.leerMasa();
            actualizarVistas();
        });

        spMasa.setPreferredSize(new Dimension(90, 26));
        spMasa.setMaximumSize(new Dimension(90, 26));
        JLabel etiquetaMasa = new JLabel("Masa a colocar:");
        etiquetaMasa.setForeground(TINTA);

        JPanel contenido = columna(
                filaEstado(luzBalanza, lblMasa),
                fila(etiquetaMasa, spMasa),
                fila(colocar, retirar),
                fila(tarar, leer),
                nota(lblBalanzaDetalle));

        return tarjeta("2 · Balanza de precisión", "BalanzaDePrecision · «external input/output device»",
                "ENTRADA / SALIDA", ENTRADA, balanzaIO.descripcionDispositivo(), contenido);
    }

    private JPanel tarjetaBuzzer() {
        JButton activar = boton("Activar alarma");
        JButton silenciar = botonSecundario("Silenciar");

        activar.addActionListener(e -> {
            buzzerOutput.activar();
            actualizarVistas();
        });
        silenciar.addActionListener(e -> {
            buzzerOutput.desactivar();
            actualizarVistas();
        });

        JPanel contenido = columna(
                filaEstado(luzBuzzer, lblBuzzer),
                fila(activar, silenciar),
                nota(lblBuzzerDetalle));

        return tarjeta("3 · Alarma sonora", "Buzzer · «external output device»",
                "SALIDA", SALIDA, buzzerOutput.descripcionDispositivo(), contenido);
    }

    private JPanel tarjetaCerradura() {
        JButton bloquear = boton("Bloquear tapa");
        JButton desbloquear = botonSecundario("Desbloquear tapa");

        bloquear.addActionListener(e -> {
            long latencia = cerraduraOutput.bloquear();
            lblCerraduraDetalle.setText("Última actuación: " + Formato.milisegundos(latencia)
                    + " · RT-06 ≤ " + CerraduraTapaOutput.LIMITE_RT06_MS + " ms");
            actualizarVistas();
        });
        desbloquear.addActionListener(e -> {
            try {
                long latencia = cerraduraOutput.desbloquear();
                lblCerraduraDetalle.setText("Última actuación: " + Formato.milisegundos(latencia)
                        + " · RT-06 ≤ " + CerraduraTapaOutput.LIMITE_RT06_MS + " ms");
            } catch (DesbloqueoNoPermitidoException ex) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Interlock de la cerradura", JOptionPane.WARNING_MESSAGE);
            }
            actualizarVistas();
        });

        JLabel interlock = nota("Desbloquear exige masa detectada (Tabla 1).");
        interlock.setForeground(AMBAR.darker());

        JPanel contenido = columna(
                filaEstado(luzCerradura, lblCerradura),
                fila(bloquear, desbloquear),
                nota(lblCerraduraDetalle),
                interlock);

        return tarjeta("4 · Cerradura de tapa", "CerraduraTapa · «external output device»",
                "SALIDA", SALIDA, cerraduraOutput.descripcionDispositivo(), contenido);
    }

    private JPanel construirRegistro() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(430, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(14, 7, 14, 14),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xD5, 0xDB, 0xE2)),
                        BorderFactory.createEmptyBorder(12, 14, 12, 14))));

        JLabel titulo = new JLabel("Registro de eventos");
        titulo.setFont(fuente(Font.BOLD, 14f));
        titulo.setForeground(TINTA);

        JLabel subtitulo = new JLabel("Instante · nivel · objeto · suceso");
        subtitulo.setFont(fuente(Font.PLAIN, 11f));
        subtitulo.setForeground(TENUE);

        JButton limpiar = botonSecundario("Limpiar");
        limpiar.addActionListener(e -> {
            registro.limpiar();
            areaRegistro.setText("");
            actualizarVistas();
        });

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        JPanel textos = columna(titulo, subtitulo);
        encabezado.add(textos, BorderLayout.WEST);
        encabezado.add(limpiar, BorderLayout.EAST);

        areaRegistro.setEditable(false);
        areaRegistro.setLineWrap(true);
        areaRegistro.setWrapStyleWord(true);
        areaRegistro.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaRegistro.setForeground(TINTA);
        areaRegistro.setBackground(new Color(0xFA, 0xFB, 0xFC));
        areaRegistro.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane scroll = new JScrollPane(areaRegistro);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE3, 0xE8, 0xEE)));

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel construirPie() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(0xE8, 0xEC, 0xF1));
        panel.setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));

        lblResumen.setFont(fuente(Font.PLAIN, 12f));
        lblResumen.setForeground(TINTA);

        JLabel ayuda = new JLabel("Actividad 3: el controlador enlazará entradas y salidas.");
        ayuda.setFont(fuente(Font.PLAIN, 11f));
        ayuda.setForeground(TENUE);

        panel.add(lblResumen, BorderLayout.WEST);
        panel.add(ayuda, BorderLayout.EAST);
        return panel;
    }

    // ------------------------------------------------------------------
    // Actualización de la vista
    // ------------------------------------------------------------------

    /** Refresca todos los indicadores a partir del estado de los dispositivos. */
    public void actualizarVistas() {
        EstadoTapa estadoTapa = sensorInput.estadoActual();
        lblTapa.setText(estadoTapa == EstadoTapa.ABIERTA ? "TAPA ABIERTA" : "TAPA CERRADA");
        lblTapa.setForeground(estadoTapa == EstadoTapa.ABIERTA ? VERDE : TINTA);
        luzTapa.setColor(estadoTapa == EstadoTapa.ABIERTA ? VERDE : GRIS);
        lblTapa.repaint();

        String detalle;
        if (sensorInput.ultimoEvento() == null) {
            detalle = "Transiciones: 0 · Sin eventos";
        } else if (!sensorInput.ultimoEvento().esCoherente()) {
            detalle = "Transiciones: " + sensorTapa.transiciones() + " · Último: lectura incoherente";
        } else {
            detalle = "Transiciones: " + sensorTapa.transiciones()
                    + " · Último: " + sensorInput.ultimoEvento().estado().descripcion();
        }
        lblTapaDetalle.setText(detalle);
        lblTapaDetalle.setForeground(sensorInput.ultimoEvento() != null
                && !sensorInput.ultimoEvento().esCoherente() ? AMBAR.darker() : TENUE);

        double masa = balanzaIO.masaActual();
        boolean detecta = balanzaIO.detectaMasa();
        lblMasa.setText("Masa leída: " + Formato.masa(masa));
        lblMasa.setForeground(TINTA);
        luzBalanza.setColor(detecta ? VERDE : GRIS);
        lblBalanzaDetalle.setText(detecta
                ? "Masa detectada en el recipiente"
                : "Recipiente vacío: sin masa detectada");
        lblMasa.repaint();

        boolean sonando = buzzerOutput.estaActivo();
        lblBuzzer.setText(sonando ? "ALARMA SONANDO" : "ALARMA SILENCIADA");
        lblBuzzer.setForeground(sonando ? ROJO : TINTA);
        luzBuzzer.setColor(sonando ? ROJO : GRIS);
        lblBuzzerDetalle.setText("Conmutaciones: " + buzzerOutput.conmutaciones());
        lblBuzzer.repaint();

        boolean bloqueada = cerraduraOutput.estaBloqueada();
        lblCerradura.setText(bloqueada ? "TAPA BLOQUEADA" : "TAPA DESBLOQUEADA");
        lblCerradura.setForeground(bloqueada ? TINTA : AMBAR.darker());
        luzCerradura.setColor(bloqueada ? GRIS : AMBAR);
        lblCerradura.repaint();

        lblResumen.setText("Eventos: " + registro.cantidad()
                + "   ·   Alertas: " + registro.cantidad(RegistroEventos.Nivel.ALERTA)
                + "   ·   Fallas: " + registro.cantidad(RegistroEventos.Nivel.FALLA));
    }

    private double valorMasa() {
        return ((Number) spMasa.getValue()).doubleValue();
    }

    // ------------------------------------------------------------------
    // Auxiliares de construcción
    // ------------------------------------------------------------------

    private static JPanel tarjeta(String titulo, String subtitulo, String direccion, Color acento,
                                  String tooltip, JPanel contenido) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        Border borde = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, acento),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xD5, 0xDB, 0xE2)),
                        BorderFactory.createEmptyBorder(10, 14, 12, 14)));
        panel.setBorder(borde);
        panel.setToolTipText(tooltip);

        JLabel etiquetaTitulo = new JLabel(titulo);
        etiquetaTitulo.setFont(fuente(Font.BOLD, 14f));
        etiquetaTitulo.setForeground(TINTA);

        JLabel insignia = new JLabel(direccion);
        insignia.setOpaque(true);
        insignia.setBackground(acento);
        insignia.setForeground(Color.WHITE);
        insignia.setFont(fuente(Font.BOLD, 9f));
        insignia.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        JPanel lineaTitulo = new JPanel(new BorderLayout(8, 0));
        lineaTitulo.setOpaque(false);
        lineaTitulo.add(etiquetaTitulo, BorderLayout.WEST);
        lineaTitulo.add(insignia, BorderLayout.EAST);

        JLabel etiquetaSubtitulo = new JLabel(subtitulo);
        etiquetaSubtitulo.setFont(fuente(Font.PLAIN, 11f));
        etiquetaSubtitulo.setForeground(TENUE);

        JPanel encabezado = columna(lineaTitulo, etiquetaSubtitulo);
        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(contenido, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel columna(Component... componentes) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (Component componente : componentes) {
            if (componente instanceof javax.swing.JComponent jc) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            panel.add(componente);
            panel.add(Box.createVerticalStrut(6));
        }
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private static JPanel fila(Component... componentes) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component componente : componentes) {
            panel.add(componente);
            panel.add(Box.createHorizontalStrut(8));
        }
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    private static JPanel filaEstado(IconoLuz luz, JLabel etiqueta) {
        etiqueta.setFont(fuente(Font.BOLD, 17f));
        etiqueta.setForeground(TINTA);
        etiqueta.setIcon(luz);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(etiqueta);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private static JLabel nota(String texto) {
        return nota(new JLabel(texto));
    }

    private static JLabel nota(JLabel etiqueta) {
        etiqueta.setFont(fuente(Font.PLAIN, 11f));
        etiqueta.setForeground(TENUE);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        return etiqueta;
    }

    private static JButton boton(String texto) {
        JButton boton = new JButton(texto);
        boton.setFont(fuente(Font.PLAIN, 12f));
        boton.setFocusPainted(false);
        return boton;
    }

    private static JButton botonSecundario(String texto) {
        JButton boton = boton(texto);
        boton.setForeground(TENUE);
        return boton;
    }

    private static Font fuente(int estilo, float tamanio) {
        return new Font("Segoe UI", estilo, Math.round(tamanio))
                .deriveFont(estilo, tamanio);
    }

    /**
     * Indicador circular usado para mostrar el estado de un dispositivo.
     */
    private static final class IconoLuz implements Icon {

        private final int diametro;
        private Color color;

        IconoLuz(int diametro) {
            this.diametro = diametro;
            this.color = GRIS;
        }

        void setColor(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component componente, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x + 1, y + 1, diametro - 2, diametro - 2);
            g2.setColor(color.darker());
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(x + 1, y + 1, diametro - 3, diametro - 3);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return diametro;
        }

        @Override
        public int getIconHeight() {
            return diametro;
        }
    }
}
