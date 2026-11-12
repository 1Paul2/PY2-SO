package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import minipc.gui.Componentes;
import minipc.gui.Tema;
import minipc.modelo.GestorProcesos;
import minipc.util.ConfiguracionSistema;

/**
 * PanelTablaEjecucion: pestaña "Tabla de ejecución" (Núcleo / Proceso / 1..80).
 * Por cada CPU hay una fila por proceso que pasó por ella y una fila de tiempo ocioso;
 * cada celda se pinta del color del proceso que ejecutó en ese segundo.
 */
public class PanelTablaEjecucion extends JPanel {

    private final ConfiguracionSistema configuracion;
    private final DefaultTableModel modeloEjecucion;
    private final JTable tablaEjecucion;        // columnas de segundos (se desplaza en horizontal)
    private final JTable tablaEjecucionFija;    // columnas Núcleo y Proceso (quedan fijas a la izquierda)
    private final JScrollPane scrollEjecucion;
    private final JLabel lblOciosoEjecucion;
    private int segundosMostrados = -1;
    private int segundoActual = 0;

    // E: configuracion (ConfiguracionSistema) - da la cantidad de CPU cuando no hay programas
    // S: no aplica (constructor)
    // R: las columnas Núcleo y Proceso quedan fijas; los segundos se desplazan en horizontal
    public PanelTablaEjecucion(ConfiguracionSistema configuracion){
        super(new BorderLayout(0, 6));
        setOpaque(false);
        this.configuracion = configuracion;
        modeloEjecucion = new DefaultTableModel(0, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        tablaEjecucion = new JTable(modeloEjecucion);
        tablaEjecucionFija = new JTable(modeloEjecucion);
        for (JTable t : new JTable[]{tablaEjecucion, tablaEjecucionFija}) {
            Componentes.estilizarTabla(t);
            t.setAutoCreateColumnsFromModel(false);
            t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            t.setRowHeight(28);
            t.setIntercellSpacing(new Dimension(0, 0));
            t.setShowGrid(false);
            t.setRowSelectionAllowed(false);
            t.setDefaultRenderer(Object.class, new RenderEjecucion());
        }
        tablaEjecucionFija.setFillsViewportHeight(false);

        scrollEjecucion = Componentes.scroll(tablaEjecucion);
        scrollEjecucion.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollEjecucion.setRowHeaderView(tablaEjecucionFija);
        scrollEjecucion.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, tablaEjecucionFija.getTableHeader());
        scrollEjecucion.getRowHeader().setBackground(Color.WHITE);

        lblOciosoEjecucion = new JLabel(" ");
        lblOciosoEjecucion.setFont(Tema.PEQUENA);
        lblOciosoEjecucion.setForeground(Tema.TEXTO_SUAVE);
        JLabel ayuda = new JLabel("Cada columna es 1 segundo · el recuadro marca el segundo actual");
        ayuda.setFont(Tema.PEQUENA);
        ayuda.setForeground(Tema.TEXTO_SUAVE);
        JPanel pie = new JPanel(new BorderLayout(12, 0));
        pie.setOpaque(false);
        pie.add(ayuda, BorderLayout.WEST);
        pie.add(lblOciosoEjecucion, BorderLayout.EAST);

        add(scrollEjecucion, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
        configurarColumnas(80);
    }

    // E: segundos (int) - cantidad de columnas de tiempo a mostrar
    // S: no aplica (void)
    // R: rehace las columnas de ambas tablas; solo se llama cuando cambia la cantidad de segundos
    private void configurarColumnas(int segundos){
        Object[] titulos = new Object[segundos + 2];
        titulos[0] = "Núcleo";
        titulos[1] = "Proceso";
        for (int s = 1; s <= segundos; s++) titulos[s + 1] = String.valueOf(s);
        modeloEjecucion.setColumnIdentifiers(titulos);

        tablaEjecucionFija.createDefaultColumnsFromModel();
        while (tablaEjecucionFija.getColumnCount() > 2)
            tablaEjecucionFija.removeColumn(tablaEjecucionFija.getColumnModel().getColumn(2));
        tablaEjecucionFija.getColumnModel().getColumn(0).setPreferredWidth(70);
        tablaEjecucionFija.getColumnModel().getColumn(1).setPreferredWidth(90);
        tablaEjecucionFija.setPreferredScrollableViewportSize(new Dimension(160, 0));

        tablaEjecucion.createDefaultColumnsFromModel();
        for (int i = 0; i < 2; i++)
            tablaEjecucion.removeColumn(tablaEjecucion.getColumnModel().getColumn(0));
        DefaultTableCellRenderer cabecera = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                boolean actual = String.valueOf(segundoActual).equals(String.valueOf(v));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(Tema.PEQUENA_B);
                l.setOpaque(true);
                l.setForeground(actual ? Color.WHITE : Tema.VERDE_OSCURO);
                l.setBackground(actual ? Tema.VERDE : Tema.VERDE_CLARO);
                l.setBorder(new EmptyBorder(6, 0, 6, 0));
                return l;
            }
        };
        for (int i = 0; i < tablaEjecucion.getColumnCount(); i++) {
            tablaEjecucion.getColumnModel().getColumn(i).setPreferredWidth(30);
            tablaEjecucion.getColumnModel().getColumn(i).setHeaderRenderer(cabecera);
        }
        segundosMostrados = segundos;
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: arma una fila por cada proceso que pasó por cada núcleo, más una fila "Ocioso" por núcleo.
    //    Se muestran al menos 80 segundos; si la ejecución dura más, se agregan columnas.
    public void actualizar(GestorProcesos gestor){
        List<int[]> historial = gestor == null ? new ArrayList<>() : gestor.getHistorial();
        int segundos = Math.max(80, historial.size());
        segundoActual = historial.size();
        if (segundos != segundosMostrados) configurarColumnas(segundos);
        modeloEjecucion.setRowCount(0);

        int cantidad = gestor == null ? configuracion.getCantidadCpus() : gestor.getCantidadCpus();
        StringBuilder ocio = new StringBuilder("Ocioso:");
        for (int cpu = 0; cpu < cantidad; cpu++) {
            TreeSet<Integer> procesos = new TreeSet<>();
            int ocioso = 0;
            for (int[] seg : historial) {
                int id = cpu < seg.length ? seg[cpu] : 0;
                if (id > 0) procesos.add(id); else ocioso++;
            }
            boolean primera = true;
            for (int id : procesos) {
                Object[] fila = new Object[segundos + 2];
                fila[0] = primera ? "CPU " + (cpu + 1) : "";
                fila[1] = id;
                for (int s = 0; s < historial.size(); s++)
                    if (cpu < historial.get(s).length && historial.get(s)[cpu] == id) fila[s + 2] = id;
                modeloEjecucion.addRow(fila);
                primera = false;
            }
            Object[] filaOcio = new Object[segundos + 2];
            filaOcio[0] = primera ? "CPU " + (cpu + 1) : "";
            filaOcio[1] = "Ocioso";
            for (int s = 0; s < historial.size(); s++)
                if (cpu >= historial.get(s).length || historial.get(s)[cpu] == 0) filaOcio[s + 2] = -1;
            modeloEjecucion.addRow(filaOcio);
            ocio.append("  CPU ").append(cpu + 1).append(": ").append(ocioso).append(" s");
        }
        lblOciosoEjecucion.setText(historial.isEmpty() ? " " : ocio.toString());
        tablaEjecucion.getTableHeader().repaint();

        // Sigue al segundo actual mientras se ejecuta
        if (segundoActual > 0) {
            Rectangle celda = tablaEjecucion.getCellRect(0, segundoActual - 1, true);
            celda.y = scrollEjecucion.getViewport().getViewPosition().y;
            tablaEjecucion.scrollRectToVisible(celda);
        }
    }

    /**
     * La celda se pinta del color del proceso si estuvo en ese núcleo en ese segundo;
     * los segundos ociosos van en gris y el segundo actual lleva un recuadro.
     */
    private class RenderEjecucion extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, "", false, false, r, c);
            int columnaModelo = t.convertColumnIndexToModel(c);
            Object nucleo = t.getModel().getValueAt(r, 0);
            boolean inicioGrupo = r > 0 && nucleo != null && !String.valueOf(nucleo).isEmpty();
            l.setIcon(null);
            l.setToolTipText(null);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setForeground(Tema.TEXTO);
            l.setFont(Tema.NORMAL);
            Color fondo = Color.WHITE;

            if (columnaModelo == 0) {
                l.setText(v == null ? "" : String.valueOf(v));
                l.setFont(Tema.NEGRITA);
                l.setHorizontalAlignment(SwingConstants.LEFT);
            } else if (columnaModelo == 1) {
                l.setHorizontalAlignment(SwingConstants.LEFT);
                if (v instanceof Integer) {
                    l.setText("P" + v);
                    l.setFont(Tema.NEGRITA);
                    l.setIcon(new Componentes.IconoPunto(Tema.colorProceso((Integer) v), 9));
                    l.setIconTextGap(6);
                } else {
                    l.setText(v == null ? "" : String.valueOf(v));
                    l.setForeground(Tema.TEXTO_SUAVE);
                }
            } else {
                int segundo = columnaModelo - 1;
                if (v instanceof Integer && (Integer) v > 0) {
                    int id = (Integer) v;
                    fondo = Tema.colorProceso(id);
                    l.setToolTipText("Segundo " + segundo + " · P" + id + " en ejecución");
                } else if (v instanceof Integer) {
                    fondo = new Color(0xE3E9E5);
                    l.setText("·");
                    l.setForeground(Tema.TEXTO_SUAVE);
                    l.setToolTipText("Segundo " + segundo + " · núcleo ocioso");
                } else if (segundo > segundoActual) {
                    fondo = new Color(0xFAFBFA);
                }
            }
            l.setBackground(fondo);

            Color linea = new Color(0xEEF2EF);
            Color grupo = new Color(0xC9D3CD);
            int arriba = inicioGrupo ? 2 : 1;
            javax.swing.border.Border borde = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(arriba, 0, 0, columnaModelo >= 2 ? 1 : 0,
                    inicioGrupo ? grupo : linea),
                new EmptyBorder(0, columnaModelo < 2 ? 8 : 0, 0, 0));
            if (columnaModelo >= 2 && columnaModelo - 1 == segundoActual) {
                borde = BorderFactory.createCompoundBorder(borde,
                    BorderFactory.createMatteBorder(0, 2, 0, 2, Tema.VERDE_OSCURO));
            }
            l.setBorder(borde);
            return l;
        }
    }
}
