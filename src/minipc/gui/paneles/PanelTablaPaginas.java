package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import minipc.gui.Componentes;
import minipc.hardware.GestorMemoria;
import minipc.modelo.GestorProcesos;

/**
 * PanelTablaPaginas: pestaña "Tabla de páginas". Por cada página de cada proceso dice
 * en qué marco de RAM está y, si fue desalojada, en qué posición del disco.
 */
public class PanelTablaPaginas extends JPanel {

    private final DefaultTableModel modeloPaginas;

    // E: no aplica
    // S: no aplica (constructor)
    // R: ninguna
    public PanelTablaPaginas(){
        super(new BorderLayout());
        setOpaque(false);
        modeloPaginas = new DefaultTableModel(new String[]{"Proceso", "Página", "Base RAM", "Base disco"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        JTable tablaPaginas = new JTable(modeloPaginas);
        Componentes.estilizarTabla(tablaPaginas);
        add(Componentes.scroll(tablaPaginas), BorderLayout.CENTER);
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: ninguna
    public void actualizar(GestorProcesos gestor){
        modeloPaginas.setRowCount(0);
        if (gestor == null) return;
        for (GestorMemoria.Pagina pagina : gestor.getGestorMemoria().getPaginas())
            modeloPaginas.addRow(new Object[]{"P" + pagina.proceso().getIdProceso(), pagina.numero(),
                pagina.baseRam() < 0 ? "-" : pagina.baseRam(), pagina.baseDisco() < 0 ? "-" : pagina.baseDisco()});
    }
}
