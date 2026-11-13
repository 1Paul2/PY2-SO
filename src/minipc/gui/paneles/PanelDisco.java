package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import minipc.gui.Componentes;
import minipc.gui.Tema;
import minipc.hardware.Disco;
import minipc.hardware.GestorMemoria;
import minipc.modelo.GestorProcesos;
import minipc.util.ConfiguracionSistema;

/**
 * PanelDisco: pestaña "Disco". Muestra cada posición del almacenamiento secundario con
 * su zona: índice de archivos, archivos o memoria virtual (con la página que guarda).
 */
public class PanelDisco extends JPanel {

    private final ConfiguracionSistema configuracion;
    private final DefaultTableModel modeloDisco;
    private final JTable tablaDisco;
    private final JLabel lblResumen = new JLabel(" ");

    // E: configuracion (ConfiguracionSistema) - da el tamaño del disco y de las páginas
    // S: no aplica (constructor)
    // R: ninguna
    public PanelDisco(ConfiguracionSistema configuracion){
        super(new BorderLayout(0, 6));
        setOpaque(false);
        this.configuracion = configuracion;
        lblResumen.setFont(Tema.PEQUENA_B);
        lblResumen.setForeground(Tema.TEXTO_SUAVE);
        add(lblResumen, BorderLayout.NORTH);
        modeloDisco = new DefaultTableModel(new String[]{"Pos", "Valor en disco", "Zona"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        tablaDisco = new JTable(modeloDisco);
        Componentes.estilizarTabla(tablaDisco);
        tablaDisco.setRowHeight(22);
        RenderDisco renderDisco = new RenderDisco();
        for (int i = 0; i < 3; i++) tablaDisco.getColumnModel().getColumn(i).setCellRenderer(renderDisco);
        tablaDisco.getColumnModel().getColumn(0).setPreferredWidth(64);
        tablaDisco.getColumnModel().getColumn(0).setMinWidth(60);
        tablaDisco.getColumnModel().getColumn(1).setPreferredWidth(220);
        tablaDisco.getColumnModel().getColumn(2).setPreferredWidth(80);
        add(Componentes.scroll(tablaDisco), BorderLayout.CENTER);
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: ninguna
    public void actualizar(GestorProcesos gestor){
        if (gestor == null) {
            modeloDisco.setRowCount(0);
            lblResumen.setText(configuracion.getAlmacenamientoSecundario() + " posiciones");
            return;
        }
        Disco d = gestor.getDisco();
        int total = d.getTamanoTotal();
        lblResumen.setText(total + " posiciones · memoria virtual usada " + d.virtualUsada() + " / " + d.getTamanoMemoriaVirtual());
        if (modeloDisco.getRowCount() != total) modeloDisco.setRowCount(total);
        int inicioVirtual = total - d.getTamanoMemoriaVirtual();
        String[] duenosVirtuales = new String[total];
        for (GestorMemoria.Pagina pagina : gestor.getGestorMemoria().getPaginas()) {
            if (pagina.baseDisco() >= 0) {
                int tamano = configuracion.getTamanoPagina();
                for (int k = 0; k < tamano; k++) duenosVirtuales[pagina.baseDisco() + k] = "P"
                    + pagina.proceso().getIdProceso() + " pág " + pagina.numero();
            }
        }
        for (int i = 0; i < total; i++) {
            String zona = i < d.getTamanoIndice() ? "Índice" : (i >= inicioVirtual ? "Virtual" : "Archivos");
            modeloDisco.setValueAt(i, i, 0);
            modeloDisco.setValueAt(d.leer(i), i, 1);
            modeloDisco.setValueAt(duenosVirtuales[i] == null ? zona : duenosVirtuales[i], i, 2);
        }
        tablaDisco.repaint();
    }

    /** Tabla de disco: índice en verde, memoria virtual en morado. */
    private static class RenderDisco extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
            String zona = String.valueOf(t.getModel().getValueAt(r, 2));
            String valor = String.valueOf(t.getModel().getValueAt(r, 1));
            Color fondo = Color.WHITE;
            if ("Índice".equals(zona)) fondo = valor.isEmpty() ? Tema.aclarar(Tema.VERDE, 0.92f) : Tema.VERDE_CLARO;
            else if ("Virtual".equals(zona)) fondo = Tema.aclarar(Tema.MORADO, 0.92f);
            else if (!valor.isEmpty()) fondo = Tema.aclarar(Tema.MORADO, 0.8f);
            l.setBackground(s ? Tema.VERDE_CLARO : fondo);
            l.setForeground(c == 2 ? Tema.TEXTO_SUAVE : Tema.TEXTO);
            l.setFont(c == 1 ? Tema.MONO_NORMAL : Tema.PEQUENA);
            l.setBorder(new EmptyBorder(0, 8, 0, 8));
            return l;
        }
    }
}
