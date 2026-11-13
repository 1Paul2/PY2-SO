package minipc.gui;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Componentes: piezas visuales reutilizables de la interfaz (tarjetas con
 * esquinas redondeadas, botones planos, distintivos de estado, barras de uso
 * y estilo de tablas).
 */
public final class Componentes {

    private Componentes() {}

    // ======================= Tarjeta =======================

    /** Panel blanco con esquinas redondeadas, franja de color y título. */
    public static class Tarjeta extends JPanel {
        private final JPanel contenido;
        private final JLabel lblExtra;

        // E: titulo (String), acento (Color) - color de la franja y del punto del título
        // S: no aplica (constructor)
        // R: el contenido se agrega con getContenido()
        public Tarjeta(String titulo, Color acento){
            super(new BorderLayout(0, 8));
            setOpaque(false);
            setBorder(new BordeRedondeado(acento));

            JPanel encabezado = new JPanel(new BorderLayout());
            encabezado.setOpaque(false);
            JLabel lbl = new JLabel(titulo);
            lbl.setFont(Tema.TITULO_CARD);
            lbl.setForeground(Tema.TEXTO);
            lbl.setIcon(new IconoPunto(acento, 10));
            lbl.setIconTextGap(8);
            encabezado.add(lbl, BorderLayout.WEST);
            lblExtra = new JLabel();
            lblExtra.setFont(Tema.PEQUENA_B);
            lblExtra.setForeground(Tema.TEXTO_SUAVE);
            encabezado.add(lblExtra, BorderLayout.EAST);
            add(encabezado, BorderLayout.NORTH);

            contenido = new JPanel(new BorderLayout());
            contenido.setOpaque(false);
            add(contenido, BorderLayout.CENTER);
        }

        public JPanel getContenido(){ return contenido; }

        // Texto pequeño a la derecha del título (p. ej. "3 / 5")
        public void setExtra(String texto){ lblExtra.setText(texto); }

        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.TARJETA);
            g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 16, 16));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Borde redondeado con una franja de color arriba. */
    public static class BordeRedondeado extends AbstractBorder {
        private final Color acento;
        public BordeRedondeado(Color acento){ this.acento = acento; }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape forma = new RoundRectangle2D.Float(x + 1, y + 1, w - 3, h - 3, 16, 16);
            g2.setClip(forma);
            g2.setColor(acento);
            g2.fillRect(x, y, w, 4);
            g2.setClip(null);
            g2.setColor(Tema.BORDE);
            g2.draw(forma);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c){ return new Insets(14, 14, 12, 14); }
    }

    /** Ícono circular de color (para títulos y leyendas). */
    public static class IconoPunto implements Icon {
        private final Color color;
        private final int tam;
        public IconoPunto(Color color, int tam){ this.color = color; this.tam = tam; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, tam, tam);
            g2.dispose();
        }
        @Override public int getIconWidth(){ return tam; }
        @Override public int getIconHeight(){ return tam; }
    }

    // ======================= Botón =======================

    /** Botón plano redondeado con color propio y efecto al pasar el mouse. */
    public static class Boton extends JButton {
        private Color base;
        private boolean encima = false;

        public Boton(String texto, Color base){
            super(texto);
            this.base = base;
            setFont(Tema.NEGRITA);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 16, 9, 16));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e){ encima = true; repaint(); }
                @Override public void mouseExited(MouseEvent e){ encima = false; repaint(); }
            });
        }

        public void setColorBase(Color c){ this.base = c; repaint(); }

        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled() ? new Color(0xCBD5E1) : (encima ? base.darker() : base);
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ======================= Distintivo (badge) =======================

    /** Etiqueta con forma de píldora y colores según el estado. */
    public static class Distintivo extends JLabel {
        private Color fondo = Tema.GRIS_CLARO;

        public Distintivo(){
            setFont(Tema.PEQUENA_B);
            setHorizontalAlignment(CENTER);
            setBorder(new EmptyBorder(3, 10, 3, 10));
        }

        // E: estado (String) - texto a mostrar; se colorea con Tema.coloresEstado
        // S: no aplica (void)
        // R: ninguna
        public void setEstado(String estado){
            Color[] c = Tema.coloresEstado(estado);
            setText(estado == null ? "-" : estado);
            setForeground(c[0]);
            fondo = c[1];
            repaint();
        }

        public void setColores(String texto, Color frente, Color fondo){
            setText(texto);
            setForeground(frente);
            this.fondo = fondo;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int w = Math.min(getWidth() - 2, fm.stringWidth(getText()) + 20);
            int h = Math.min(getHeight() - 2, fm.getHeight() + 6);
            int x;
            if (getHorizontalAlignment() == LEFT) x = 0;
            else if (getHorizontalAlignment() == RIGHT) x = getWidth() - w - 1;
            else x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;
            g2.setColor(fondo);
            g2.fillRoundRect(x, y, w, h, h, h);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Renderizador de celdas que muestra el estado como distintivo de color. */
    public static class RenderEstado extends DefaultTableCellRenderer {
        private final Distintivo d = new Distintivo();
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col){
            d.setEstado(v == null ? "-" : v.toString());
            d.setOpaque(true);
            d.setBackground(sel ? Tema.VERDE_CLARO : (fila % 2 == 0 ? Color.WHITE : Tema.FILA_ALTERNA));
            return d;
        }
    }

    // ======================= Barra de uso =======================

    /** Barra de progreso redondeada con etiqueta y valor. */
    public static class BarraUso extends JPanel {
        private final JLabel lblValor;
        private final Color color;
        private double fraccion = 0;

        public BarraUso(String nombre, Color color){
            super(new BorderLayout(0, 4));
            this.color = color;
            setOpaque(false);
            JPanel arriba = new JPanel(new BorderLayout());
            arriba.setOpaque(false);
            JLabel lbl = new JLabel(nombre);
            lbl.setFont(Tema.NORMAL);
            lbl.setForeground(Tema.TEXTO);
            lbl.setIcon(new IconoPunto(color, 8));
            lbl.setIconTextGap(6);
            lblValor = new JLabel("-");
            lblValor.setFont(Tema.PEQUENA_B);
            lblValor.setForeground(Tema.TEXTO_SUAVE);
            arriba.add(lbl, BorderLayout.WEST);
            arriba.add(lblValor, BorderLayout.EAST);
            add(arriba, BorderLayout.NORTH);

            JComponent barra = new JComponent() {
                @Override protected void paintComponent(Graphics g){
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int h = getHeight();
                    g2.setColor(Tema.aclarar(color, 0.85f));
                    g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                    int w = (int) Math.round(getWidth() * Math.max(0, Math.min(1, fraccion)));
                    if (w > 0) {
                        g2.setColor(color);
                        g2.fillRoundRect(0, 0, Math.max(w, h), h, h, h);
                    }
                    g2.dispose();
                }
            };
            barra.setPreferredSize(new Dimension(10, 8));
            add(barra, BorderLayout.CENTER);
        }

        // E: usado, total (int)
        // S: no aplica (void)
        // R: total > 0
        public void setValor(int usado, int total){
            fraccion = total <= 0 ? 0 : (double) usado / total;
            lblValor.setText(usado + " / " + total);
            repaint();
        }
    }

    // ======================= Tablas =======================

    // E: tabla (JTable)
    // S: no aplica (void)
    // R: aplica el estilo del tema: filas altas, sin rejilla vertical, encabezado verde claro
    public static void estilizarTabla(JTable tabla){
        tabla.setFont(Tema.NORMAL);
        tabla.setRowHeight(26);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(0xEEF2EF));
        tabla.setSelectionBackground(Tema.VERDE_CLARO);
        tabla.setSelectionForeground(Tema.TEXTO);
        tabla.setFillsViewportHeight(true);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setDefaultEditor(Object.class, null);
        JTableHeader h = tabla.getTableHeader();
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                l.setFont(Tema.PEQUENA_B);
                l.setForeground(Tema.VERDE_OSCURO);
                l.setBackground(Tema.VERDE_CLARO);
                l.setBorder(new EmptyBorder(6, 8, 6, 8));
                return l;
            }
        });
    }

    // E: tabla (JTable)
    // S: JScrollPane - la tabla dentro de un scroll sin borde
    // R: ninguna
    public static JScrollPane scroll(JComponent c){
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setPreferredSize(new Dimension(200, 120));   // deja que el layout decida el alto
        return sp;
    }

    // ======================= Utilidades de armado =======================

    // E: partes (Component[]), pesos (double[]) - cuánto alto toma cada parte
    // S: JPanel - columna con las partes una debajo de otra
    // R: partes y pesos tienen el mismo largo
    public static JPanel columna(Component[] partes, double[] pesos){
        JPanel col = new JPanel(new GridBagLayout());
        col.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1;
        for (int i = 0; i < partes.length; i++) {
            g.gridy = i;
            g.weighty = pesos[i];
            g.insets = new Insets(0, 0, i < partes.length - 1 ? 12 : 0, 0);
            col.add(partes[i], g);
        }
        return col;
    }

    // E: t (String) - texto
    // S: JLabel - etiqueta con la fuente y el color normales del tema
    // R: ninguna
    public static JLabel etiqueta(String t){
        JLabel l = new JLabel(t);
        l.setFont(Tema.NORMAL);
        l.setForeground(Tema.TEXTO);
        return l;
    }
    // E: doc (StyledDocument), texto (String), color (Color), negrita y cursiva (boolean)
    // S: no aplica (void)
    // R: agrega el texto al final con la fuente monoespaciada del tema
    public static void agregarTexto(javax.swing.text.StyledDocument doc, String texto, Color color, boolean negrita, boolean cursiva){
        javax.swing.text.SimpleAttributeSet a = new javax.swing.text.SimpleAttributeSet();
        javax.swing.text.StyleConstants.setFontFamily(a, Tema.MONO_NORMAL.getFamily());
        javax.swing.text.StyleConstants.setFontSize(a, 13);
        javax.swing.text.StyleConstants.setForeground(a, color);
        javax.swing.text.StyleConstants.setBold(a, negrita);
        javax.swing.text.StyleConstants.setItalic(a, cursiva);
        try {
            doc.insertString(doc.getLength(), texto, a);
        } catch (javax.swing.text.BadLocationException ignorada) { }
    }

    // ======================= Sección desplegable =======================

    /** Tarjeta cuyo contenido se muestra u oculta al hacer clic en el título (▸ / ▾). */
    public static class Desplegable extends JPanel {
        private final JLabel lblTitulo;
        private final JComponent contenido;
        private String titulo;
        private boolean abierto;

        // E: titulo (String), acento (Color), contenido (JComponent), abierto (boolean) - estado inicial
        // S: no aplica (constructor)
        // R: ninguna
        public Desplegable(String titulo, Color acento, JComponent contenido, boolean abierto){
            super(new BorderLayout(0, 8));
            setOpaque(false);
            setBorder(new BordeRedondeado(acento));
            this.contenido = contenido;
            lblTitulo = new JLabel();
            lblTitulo.setFont(Tema.TITULO_CARD);
            lblTitulo.setForeground(Tema.TEXTO);
            lblTitulo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblTitulo.setToolTipText("Clic para mostrar u ocultar");
            lblTitulo.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e){ setAbierto(!Desplegable.this.abierto); }
            });
            add(lblTitulo, BorderLayout.NORTH);
            add(contenido, BorderLayout.CENTER);
            this.titulo = titulo;
            setAbierto(abierto);
        }

        public void setTitulo(String titulo){
            this.titulo = titulo;
            lblTitulo.setText((abierto ? "▾  " : "▸  ") + titulo);
        }

        public void setAbierto(boolean abierto){
            this.abierto = abierto;
            contenido.setVisible(abierto);
            setTitulo(titulo);
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.TARJETA);
            g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 16, 16));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
