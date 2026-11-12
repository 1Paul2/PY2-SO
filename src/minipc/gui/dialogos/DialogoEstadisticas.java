package minipc.gui.dialogos;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import minipc.gui.Componentes;
import minipc.gui.Componentes.Boton;
import minipc.gui.Componentes.RenderEstado;
import minipc.gui.Componentes.Tarjeta;
import minipc.gui.Tema;
import minipc.modelo.GestorProcesos;

/**
 * DialogoEstadisticas: ventana con dos pestañas. "Esta ejecución" muestra los tiempos de
 * cada proceso (llegada, inicio, final, Ts, Tr y Tr/Ts) y "Acumulado" resume todas las
 * ejecuciones hechas, una fila por ejecución.
 */
public final class DialogoEstadisticas {

    private DialogoEstadisticas() {}

    // E: padre (Frame); gestor (GestorProcesos) - ejecución actual, puede ser null;
    //    acumulado (List<String[]>) - un resumen por ejecución terminada
    // S: no aplica (void)
    // R: debe haber una ejecución actual con procesos o al menos una ejecución acumulada
    public static void mostrar(Frame padre, GestorProcesos gestor, List<String[]> acumulado){
        boolean hayActual = gestor != null && !gestor.getTodosLosProcesos().isEmpty();
        JDialog dlg = new JDialog(padre, "Estadísticas de ejecución", true);
        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(new EmptyBorder(16, 16, 16, 16));

        DefaultTableCellRenderer centro = new DefaultTableCellRenderer();
        centro.setHorizontalAlignment(SwingConstants.CENTER);
        JTabbedPane pestanas = new JTabbedPane();

        // ---- Esta ejecución ----
        if (hayActual) {
            String[] r = gestor.getResumen();
            JPanel resumen = new JPanel(new GridLayout(1, 4, 12, 0));
            resumen.setOpaque(false);
            resumen.add(tarjetaDato("Algoritmo", r[0], Tema.VERDE));
            resumen.add(tarjetaDato("Tiempo total", r[4] + " s", Tema.MORADO));
            resumen.add(tarjetaDato("Tiempo ocioso", r[5] + " s", Tema.AZUL));
            resumen.add(tarjetaDato("Tr/Ts promedio", r[7], Tema.AMBAR));

            DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"Proceso", "Programa", "Llegada", "Inicio", "Final", "Ts (ráfaga)", "Tr", "Tr/Ts", "Estado"}, 0) {
                @Override public boolean isCellEditable(int f, int c){ return false; }
            };
            for (String[] f : gestor.getEstadisticas()) modelo.addRow(f);
            JTable tabla = new JTable(modelo);
            Componentes.estilizarTabla(tabla);
            for (int i = 2; i <= 7; i++) tabla.getColumnModel().getColumn(i).setCellRenderer(centro);
            tabla.getColumnModel().getColumn(8).setCellRenderer(new RenderEstado());

            JPanel actual = new JPanel(new BorderLayout(0, 12));
            actual.setOpaque(false);
            actual.setBorder(new EmptyBorder(10, 0, 0, 0));
            actual.add(resumen, BorderLayout.NORTH);
            actual.add(Componentes.scroll(tabla), BorderLayout.CENTER);
            actual.add(Componentes.etiqueta("Tiempos en segundos desde 0.  Tr = Final − Llegada.  Ts = ráfaga (líneas a procesar)."),
                BorderLayout.SOUTH);
            pestanas.addTab("Esta ejecución", actual);
        }

        // ---- Acumulado de todas las ejecuciones ----
        DefaultTableModel modeloAcum = new DefaultTableModel(
            new String[]{"#", "Algoritmo", "CPU", "Procesos", "Inicio", "Final", "Ocioso", "Tr promedio", "Tr/Ts promedio"}, 0) {
            @Override public boolean isCellEditable(int f, int c){ return false; }
        };
        for (int i = 0; i < acumulado.size(); i++) {
            String[] e = acumulado.get(i);
            Object[] fila = new Object[e.length + 1];
            fila[0] = i + 1;
            System.arraycopy(e, 0, fila, 1, e.length);
            modeloAcum.addRow(fila);
        }
        JTable tablaAcum = new JTable(modeloAcum);
        Componentes.estilizarTabla(tablaAcum);
        for (int i = 0; i < modeloAcum.getColumnCount(); i++) tablaAcum.getColumnModel().getColumn(i).setCellRenderer(centro);
        pestanas.addTab("Acumulado (" + acumulado.size() + ")", Componentes.scroll(tablaAcum));

        raiz.add(pestanas, BorderLayout.CENTER);

        Boton cerrar = new Boton("Cerrar", Tema.VERDE);
        cerrar.addActionListener(e -> dlg.dispose());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pie.setOpaque(false);
        pie.add(cerrar);
        raiz.add(pie, BorderLayout.SOUTH);

        dlg.setContentPane(raiz);
        dlg.setSize(960, 500);
        dlg.setLocationRelativeTo(padre);
        dlg.setVisible(true);
    }

    private static JPanel tarjetaDato(String titulo, String valor, Color color){
        Tarjeta t = new Tarjeta(titulo, color);
        JLabel v = new JLabel(valor);
        v.setFont(new Font(Tema.TITULO.getFamily(), Font.BOLD, 24));
        v.setForeground(color);
        t.getContenido().add(v, BorderLayout.CENTER);
        return t;
    }
}
