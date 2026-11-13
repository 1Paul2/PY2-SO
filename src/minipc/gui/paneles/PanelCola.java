package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import minipc.gui.Componentes;
import minipc.gui.Componentes.Distintivo;
import minipc.gui.Componentes.RenderEstado;
import minipc.gui.Componentes.Tarjeta;
import minipc.gui.Tema;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;

/**
 * PanelCola: tarjeta "Cola de trabajos". Muestra todos los procesos con su estado,
 * base, alcance, ráfaga, llegada y CPU, y abajo la leyenda de colores de los estados.
 */
public class PanelCola extends Tarjeta {

    private final DefaultTableModel modeloCola;

    // E: no aplica
    // S: no aplica (constructor)
    // R: ninguna
    public PanelCola(){
        super("Cola de trabajos", Tema.VERDE);
        modeloCola = new DefaultTableModel(new String[]{"#", "Programa", "Estado", "Base", "Alcance", "Ráfaga", "Llegada (s)", "CPU"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        JTable tablaCola = new JTable(modeloCola);
        Componentes.estilizarTabla(tablaCola);
        tablaCola.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaCola.getColumnModel().getColumn(5).setPreferredWidth(65);
        tablaCola.getColumnModel().getColumn(6).setPreferredWidth(85);
        tablaCola.getColumnModel().getColumn(0).setCellRenderer(new RenderProceso());
        tablaCola.getColumnModel().getColumn(2).setCellRenderer(new RenderEstado());
        tablaCola.getColumnModel().getColumn(0).setPreferredWidth(44);
        tablaCola.getColumnModel().getColumn(1).setPreferredWidth(130);
        tablaCola.getColumnModel().getColumn(2).setPreferredWidth(100);
        tablaCola.getColumnModel().getColumn(3).setPreferredWidth(50);
        tablaCola.getColumnModel().getColumn(4).setPreferredWidth(60);
        getContenido().add(Componentes.scroll(tablaCola), BorderLayout.CENTER);
        JScrollPane estados = Componentes.scroll(crearLeyendaEstados());
        estados.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        estados.setPreferredSize(new Dimension(300, 48));
        getContenido().add(estados, BorderLayout.SOUTH);
    }

    // Correccion - Poll (observación PY1): la leyenda incluye "Finalizado con error"
    private JPanel crearLeyendaEstados(){
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        p.setOpaque(false);
        for (String e : new String[]{"Nuevo", "Preparado", "Ejecución", "En espera", BCP.FINALIZADO, BCP.FINALIZADO_ERROR}) {
            Distintivo d = new Distintivo();
            d.setEstado(e);
            p.add(d);
        }
        return p;
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: ninguna
    // Correccion - Poll (observación PY1): "Finalizado con error" también oculta la base, igual que "Finalizado"
    public void actualizar(GestorProcesos gestor){
        modeloCola.setRowCount(0);
        if (gestor == null) {
            setExtra("");
            return;
        }
        for (BCP b : gestor.getTodosLosProcesos()) {
            modeloCola.addRow(new Object[]{
                b.getIdProceso(),
                b.getArchivosAbiertos().isEmpty() ? "-" : b.getArchivosAbiertos().get(0),
                b.getEstado(),
                b.getDireccionBase() >= 0 && !b.estaFinalizado() ? b.getDireccionBase() : "-",
                b.getTamanioProceso(),
                b.getRafaga(),
                b.getTiempoLlegada(),
                b.getCpuId() > 0 ? "CPU " + b.getCpuId() : "-"
            });
        }
        int trabajos = gestor.getPlanificador().getListaTrabajos().tamanio();
        setExtra(gestor.getTodosLosProcesos().size() + " procesos · " + trabajos + " esperando memoria");
    }

    /** Columna "#" de la cola: punto del color del proceso + "P1". */
    private static class RenderProceso extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, "P" + v, s, f, r, c);
            l.setFont(Tema.NEGRITA);
            l.setIcon(new Componentes.IconoPunto(Tema.colorProceso((Integer) v), 9));
            l.setIconTextGap(6);
            l.setBackground(s ? Tema.VERDE_CLARO : (r % 2 == 0 ? Color.WHITE : Tema.FILA_ALTERNA));
            return l;
        }
    }
}
