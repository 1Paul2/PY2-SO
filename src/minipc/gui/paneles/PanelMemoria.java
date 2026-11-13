package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import minipc.gui.Componentes;
import minipc.gui.Tema;
import minipc.hardware.CPU;
import minipc.hardware.GestorMemoria;
import minipc.hardware.Memoria;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;

/**
 * PanelMemoria: pestaña "RAM física". A la izquierda el mapa de bloques de colores
 * (MapaMemoria) y a la derecha la tabla posición por posición, con el dueño de cada
 * posición y el PC de la CPU elegida resaltado.
 */
public class PanelMemoria extends JSplitPane {

    private final MapaMemoria mapaMemoria;
    private final DefaultTableModel modeloMemoria;
    private final JTable tablaMemoria;
    private String[] duenoPosicion = new String[0];   // "S.O.", "P1"  por posición
    private int posicionPC = -1;

    // E: no aplica
    // S: no aplica (constructor)
    // R: ninguna
    public PanelMemoria(){
        super(JSplitPane.HORIZONTAL_SPLIT);
        mapaMemoria = new MapaMemoria();

        modeloMemoria = new DefaultTableModel(new String[]{"Pos", "Valor en memoria", "Dueño"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        tablaMemoria = new JTable(modeloMemoria);
        Componentes.estilizarTabla(tablaMemoria);
        tablaMemoria.setRowHeight(22);
        RenderMemoria render = new RenderMemoria();
        for (int i = 0; i < 3; i++) tablaMemoria.getColumnModel().getColumn(i).setCellRenderer(render);
        tablaMemoria.getColumnModel().getColumn(0).setPreferredWidth(52);
        tablaMemoria.getColumnModel().getColumn(0).setMinWidth(50);
        tablaMemoria.getColumnModel().getColumn(1).setPreferredWidth(260);
        tablaMemoria.getColumnModel().getColumn(2).setPreferredWidth(64);    // "BCP P1"
        tablaMemoria.getColumnModel().getColumn(2).setMinWidth(60);

        JPanel mapaConLeyenda = new JPanel(new BorderLayout(0, 6));
        mapaConLeyenda.setOpaque(false);
        mapaConLeyenda.add(mapaMemoria, BorderLayout.CENTER);
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leyenda.setOpaque(false);
        leyenda.add(itemLeyenda("S.O. / BCP", Tema.NARANJA_SO));
        leyenda.add(itemLeyenda("Libre", new Color(0xCBD5D0)));
        leyenda.add(itemLeyenda("PC", Tema.ROJO));
        mapaConLeyenda.add(leyenda, BorderLayout.SOUTH);

        setLeftComponent(mapaConLeyenda);
        setRightComponent(Componentes.scroll(tablaMemoria));
        setResizeWeight(0.32);   // la tabla necesita espacio para leer los campos del BCP
        setBorder(null);
        setOpaque(false);
        setDividerSize(8);
    }

    private JLabel itemLeyenda(String texto, Color c){
        JLabel l = new JLabel(texto, new Componentes.IconoPunto(c, 9), SwingConstants.LEFT);
        l.setFont(Tema.PEQUENA);
        l.setForeground(Tema.TEXTO_SUAVE);
        return l;
    }

    // E: gestor (GestorProcesos) - puede ser null; cpuElegida (CPU) - la CPU cuyo PC se resalta
    // S: no aplica (void)
    // R: ninguna
    // Correccion - Poll (observación PY1): las 16 posiciones del bloque de cada BCP se marcan como
    // suyas ("BCP P1"); el valor de cada posición ya dice qué campo es ("PC=82")
    public void actualizar(GestorProcesos gestor, CPU cpuElegida){
        mapaMemoria.setGestor(gestor);
        if (gestor == null) {
            modeloMemoria.setRowCount(0);
            duenoPosicion = new String[0];
            posicionPC = -1;
            return;
        }
        Memoria m = gestor.getMemoria();
        int total = m.getTamanoTotal();
        duenoPosicion = new String[total];
        for (int i = 0; i < total; i++) duenoPosicion[i] = i < m.getInicioUsuario() ? "S.O." : "";
        for (BCP b : gestor.getTodosLosProcesos()) {
            String e = b.getEstado();
            if ("Nuevo".equals(e) || b.estaFinalizado() || b.getDireccionBase() < 0) continue;
            if (b.getDireccionBCP() >= 0) {
                for (int k = 0; k < BCP.TAMANO_BLOQUE && b.getDireccionBCP() + k < total; k++) {
                    duenoPosicion[b.getDireccionBCP() + k] = "BCP P" + b.getIdProceso();
                }
            }
        }
        for (GestorMemoria.Bloque bloque : gestor.getGestorMemoria().getBloquesFisicos()) {
            for (int i = 0; i < bloque.tamanio(); i++)
                duenoPosicion[bloque.base() + i] = (i < bloque.instrucciones() ? "P" : "Interna P") + bloque.proceso().getIdProceso();
        }
        if (modeloMemoria.getRowCount() != total) modeloMemoria.setRowCount(total);
        for (int i = 0; i < total; i++) {
            modeloMemoria.setValueAt(i, i, 0);
            modeloMemoria.setValueAt(m.leer(i), i, 1);
            modeloMemoria.setValueAt(duenoPosicion[i], i, 2);
        }

        BCP actual = cpuElegida.getBcpActual();
        int nuevoPC = actual == null ? -1 : gestor.getGestorMemoria().direccionFisica(
            actual, cpuElegida.getPC() - actual.getDireccionBase());
        if (nuevoPC != posicionPC && nuevoPC >= 0 && nuevoPC < total) {
            tablaMemoria.scrollRectToVisible(tablaMemoria.getCellRect(Math.min(total - 1, nuevoPC + 3), 0, true));
            tablaMemoria.scrollRectToVisible(tablaMemoria.getCellRect(nuevoPC, 0, true));
        }
        posicionPC = nuevoPC;
        tablaMemoria.repaint();
    }

    /** Tabla de memoria: cada fila se tiñe con el color de su dueño; el PC se resalta. */
    private class RenderMemoria extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
            String dueno = r < duenoPosicion.length ? duenoPosicion[r] : "";
            Color fondo = Color.WHITE;
            Color texto = Tema.TEXTO;
            if (dueno.startsWith("BCP")) {
                fondo = Tema.aclarar(Tema.NARANJA_SO, 0.6f);
                texto = Tema.NARANJA_SO.darker().darker();
            } else if ("S.O.".equals(dueno)) {
                fondo = Tema.aclarar(Tema.NARANJA_SO, 0.88f);
                texto = Tema.TEXTO_SUAVE;
            } else if (dueno.startsWith("P")) {
                int id = Integer.parseInt(dueno.substring(1));
                fondo = Tema.aclarar(Tema.colorProceso(id), 0.82f);
            }
            boolean esPC = r == posicionPC;
            if (esPC) {
                fondo = Tema.ROJO_CLARO;
                texto = Tema.ROJO;
            }
            l.setBackground(s ? Tema.VERDE_CLARO : fondo);
            l.setForeground(texto);
            l.setFont(c == 1 ? Tema.MONO_NORMAL : (esPC ? Tema.NEGRITA : Tema.NORMAL));
            l.setBorder(new EmptyBorder(0, 8, 0, 8));
            if (c == 0 && esPC) l.setText("▶ " + v);
            // Correccion - Poll (observación PY1): al pasar el mouse se ve el valor completo de la
            // posición (los campos del BCP pueden ser más largos que la columna)
            l.setToolTipText(dueno.isEmpty() ? String.valueOf(t.getModel().getValueAt(r, 1))
                : dueno + "  ·  " + t.getModel().getValueAt(r, 1));
            return l;
        }
    }
}
