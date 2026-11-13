package minipc.gui.paneles;

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
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import minipc.gui.Componentes;
import minipc.gui.Componentes.Distintivo;
import minipc.gui.Tema;
import minipc.hardware.CPU;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;
import minipc.util.ConfiguracionSistema;

/**
 * PanelTarjetasCpu: fila con una tarjeta por CPU (hasta 5). Cada tarjeta muestra el proceso
 * que ejecuta, su PC, la última instrucción y cuánto lleva de su quantum. Al hacer clic en una
 * tarjeta se elige esa CPU para ver su BCP y sus registros.
 */
public class PanelTarjetasCpu extends JPanel {

    private final ConfiguracionSistema configuracion;
    private final List<TarjetaCpu> tarjetas = new ArrayList<>();
    private IntConsumer alElegirCpu = i -> { };
    private int elegida = 0;

    // E: configuracion (ConfiguracionSistema) - cantidad de CPU y quantum cuando no hay programas
    // S: no aplica (constructor)
    // R: ninguna
    public PanelTarjetasCpu(ConfiguracionSistema configuracion){
        super(new GridLayout(1, GestorProcesos.MAX_CPUS, 12, 0));
        setOpaque(false);
        this.configuracion = configuracion;
    }

    // E: accion (IntConsumer) - recibe el índice de la CPU elegida (0 = CPU 1)
    // S: no aplica (void)
    // R: ninguna
    public void setAlElegirCpu(IntConsumer accion){ this.alElegirCpu = accion; }

    // E: no aplica
    // S: int - índice de la CPU elegida (0 = CPU 1)
    // R: ninguna
    public int getElegida(){ return elegida; }

    private void elegir(int indice){
        elegida = indice;
        for (int i = 0; i < tarjetas.size(); i++) tarjetas.get(i).setElegida(i == indice);
        alElegirCpu.accept(indice);
    }

    // Rehace la fila si cambió la cantidad de CPU; los lugares sobrantes quedan vacíos
    private void prepararTarjetas(int cantidad){
        if (tarjetas.size() == cantidad) return;
        removeAll();
        tarjetas.clear();
        for (int i = 0; i < GestorProcesos.MAX_CPUS; i++) {
            if (i < cantidad) {
                TarjetaCpu t = new TarjetaCpu(i);
                tarjetas.add(t);
                add(t);
            } else {
                JPanel vacio = new JPanel();
                vacio.setOpaque(false);
                add(vacio);
            }
        }
        if (elegida >= cantidad) elegida = 0;
        for (int i = 0; i < tarjetas.size(); i++) tarjetas.get(i).setElegida(i == elegida);
        revalidate();
        repaint();
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: ninguna
    public void actualizar(GestorProcesos gestor){
        int cantidad = gestor == null ? configuracion.getCantidadCpus() : gestor.getCantidadCpus();
        prepararTarjetas(cantidad);
        String algoritmo = configuracion.getAlgoritmoCpu();
        boolean usaQuantum = "RR".equals(algoritmo) || "SRR".equals(algoritmo) || "Lottery".equals(algoritmo);
        for (int i = 0; i < cantidad; i++) {
            CPU cpu = gestor == null ? null : gestor.getCpu(i);
            int segundos = gestor == null ? 0 : gestor.getSegundosEnCpu(i);
            tarjetas.get(i).mostrar(cpu, segundos, usaQuantum ? configuracion.getQuantumRR() : 0);
        }
    }

    /** Tarjeta de una CPU: borde del color del proceso y borde verde si es la elegida. */
    private class TarjetaCpu extends JPanel {
        private final JLabel lblNombre = new JLabel();
        private final Distintivo estado = new Distintivo();
        private final JLabel lblProceso = new JLabel();
        private final JLabel lblPrograma = new JLabel();
        private final JLabel lblInstruccion = new JLabel();
        private final JLabel lblTiempo = new JLabel();
        private final BarraQuantum barra = new BarraQuantum();
        private Color colorProceso = null;
        private boolean elegida = false;

        TarjetaCpu(int indice){
            super(new GridLayout(4, 1, 0, 2));
            setOpaque(false);
            setBorder(new EmptyBorder(10, 16, 10, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Clic para ver los registros y el BCP de esta CPU");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e){ elegir(indice); }
            });

            lblNombre.setText("CPU " + (indice + 1));
            lblNombre.setFont(Tema.PEQUENA_B);
            lblNombre.setForeground(Tema.TEXTO_SUAVE);
            JPanel fila1 = fila();
            fila1.add(lblNombre, BorderLayout.WEST);
            fila1.add(estado, BorderLayout.EAST);

            lblProceso.setFont(new Font(Tema.TITULO.getFamily(), Font.BOLD, 17));
            lblProceso.setForeground(Tema.TEXTO);
            lblProceso.setIconTextGap(8);
            lblPrograma.setFont(Tema.PEQUENA);
            lblPrograma.setForeground(Tema.TEXTO_SUAVE);
            JPanel fila2 = fila();
            fila2.add(lblProceso, BorderLayout.WEST);
            fila2.add(lblPrograma, BorderLayout.CENTER);
            lblPrograma.setBorder(new EmptyBorder(0, 8, 0, 0));

            lblInstruccion.setFont(Tema.MONO_NORMAL);
            lblInstruccion.setForeground(Tema.TEXTO);

            lblTiempo.setFont(Tema.PEQUENA);
            lblTiempo.setForeground(Tema.TEXTO_SUAVE);
            JPanel fila4 = fila();
            fila4.add(lblTiempo, BorderLayout.WEST);
            fila4.add(barra, BorderLayout.EAST);

            add(fila1);
            add(fila2);
            add(lblInstruccion);
            add(fila4);
        }

        private JPanel fila(){
            JPanel p = new JPanel(new BorderLayout());
            p.setOpaque(false);
            return p;
        }

        void setElegida(boolean elegida){
            this.elegida = elegida;
            repaint();
        }

        // E: cpu (CPU) - puede ser null; segundos (int) - en la CPU; quantum (int) - 0 si el algoritmo no usa
        void mostrar(CPU cpu, int segundos, int quantum){
            BCP b = cpu == null ? null : cpu.getBcpActual();
            if (b == null) {
                colorProceso = null;
                estado.setColores("Ociosa", Tema.GRIS, Tema.GRIS_CLARO);
                lblProceso.setIcon(new Componentes.IconoPunto(new Color(0xCBD5D0), 12));
                lblProceso.setText("Libre");
                lblPrograma.setText("esperando un proceso");
                lblInstruccion.setText("—");
                lblTiempo.setText(" ");
                barra.setFraccion(-1);
            } else {
                colorProceso = Tema.colorProceso(b.getIdProceso());
                estado.setEstado(b.getEstado());
                lblProceso.setIcon(new Componentes.IconoPunto(colorProceso, 12));
                lblProceso.setText("P" + b.getIdProceso());
                lblPrograma.setText(b.getArchivosAbiertos().isEmpty() ? "" : b.getArchivosAbiertos().get(0));
                String ir = cpu.getIR() == null || cpu.getIR().isEmpty() ? "-" : cpu.getIR();
                lblInstruccion.setText("PC " + cpu.getPC() + " · " + ir);
                if (quantum > 0) {
                    int enTurno = segundos == 0 ? 0 : (segundos - 1) % quantum + 1;
                    lblTiempo.setText("Quantum " + enTurno + " / " + quantum + " s");
                    barra.setFraccion((double) enTurno / quantum);
                } else {
                    lblTiempo.setText("En CPU: " + segundos + " s");
                    barra.setFraccion(-1);
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape forma = new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            g2.setColor(Tema.TARJETA);
            g2.fill(forma);
            if (colorProceso != null) {      // franja del color del proceso a la izquierda
                g2.setClip(forma);
                g2.setColor(colorProceso);
                g2.fillRect(0, 0, 6, getHeight());
                g2.setClip(null);
            }
            g2.setColor(elegida ? Tema.VERDE_OSCURO : Tema.BORDE);
            g2.setStroke(new BasicStroke(elegida ? 2.5f : 1f));
            g2.draw(forma);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Barra pequeña con cuánto del quantum lleva el proceso; se oculta si no aplica. */
    private static class BarraQuantum extends JComponent {
        private double fraccion = -1;

        BarraQuantum(){ setPreferredSize(new Dimension(70, 8)); }

        void setFraccion(double f){ fraccion = f; repaint(); }

        @Override
        protected void paintComponent(Graphics g){
            if (fraccion < 0) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = 8, y = (getHeight() - h) / 2;
            g2.setColor(new Color(0xE3E9E5));
            g2.fillRoundRect(0, y, getWidth(), h, h, h);
            int w = (int) Math.round(getWidth() * Math.min(1, fraccion));
            if (w > 0) {
                g2.setColor(fraccion >= 1 ? Tema.AMBAR : Tema.VERDE);
                g2.fillRoundRect(0, y, Math.max(w, h), h, h, h);
            }
            g2.dispose();
        }
    }
}
