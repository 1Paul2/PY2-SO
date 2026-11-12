package minipc.gui.dialogos;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;

/**
 * DialogoLlegadas: ventana para editar, de cada proceso que todavía no llegó, su tiempo
 * de llegada, sus boletos (Lottery) y su prioridad.
 */
public final class DialogoLlegadas {

    private DialogoLlegadas() {}

    // E: padre (Component) - ventana sobre la que se abre; gestor (GestorProcesos) - con procesos cargados
    // S: no aplica (void)
    // R: solo se muestran los procesos pendientes; al cancelar se conservan sus llegadas
    public static void mostrar(Component padre, GestorProcesos gestor){
        List<BCP> procesos = gestor.getPendientesLlegada();
        int maximo = GestorProcesos.MAX_SEGUNDOS_AUTOMATICO - 1;
        JPanel campos = new JPanel(new GridLayout(0, 5, 12, 8));
        campos.add(new JLabel("Proceso / archivo"));
        campos.add(new JLabel("Ráfaga (líneas)"));
        campos.add(new JLabel("Llegada (segundos desde 0)"));
        campos.add(new JLabel("Boletos de Lottery"));
        campos.add(new JLabel("Prioridad (1 = máxima)"));
        List<JSpinner> prioridades = new ArrayList<>();
        List<JSpinner> boletos = new ArrayList<>();
        List<JSpinner> entradas = new ArrayList<>();
        for (BCP b : procesos) {
            campos.add(new JLabel("P" + b.getIdProceso() + " - " + b.getArchivosAbiertos().get(0)));
            campos.add(new JLabel(String.valueOf(b.getRafaga())));
            JSpinner entrada = new JSpinner(new SpinnerNumberModel(b.getTiempoLlegada(), gestor.getReloj(), maximo, 1));
            entradas.add(entrada);
            campos.add(entrada);
            JSpinner cantidad = new JSpinner(new SpinnerNumberModel(b.getBoletos(), 1, Integer.MAX_VALUE, 1));
            boletos.add(cantidad);
            campos.add(cantidad);
            JSpinner prioridad = new JSpinner(new SpinnerNumberModel(b.getPrioridad(), 1, Integer.MAX_VALUE, 1));
            prioridades.add(prioridad);
            campos.add(prioridad);
        }
        JScrollPane scroll = new JScrollPane(campos);
        scroll.setPreferredSize(new Dimension(980, Math.min(380, 65 + procesos.size() * 35)));
        while (true) {
            int opcion = JOptionPane.showConfirmDialog(padre, scroll,
                "Tiempos de llegada · reloj actual: " + gestor.getReloj() + " s",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) return; // Conserva las llegadas por defecto.
            try {
                for (JSpinner entrada : entradas) entrada.commitEdit();
                for (JSpinner cantidad : boletos) cantidad.commitEdit();
                for (JSpinner prioridad : prioridades) prioridad.commitEdit();
                for (int i = 0; i < procesos.size(); i++)
                    gestor.configurarLlegada(procesos.get(i), ((Number) entradas.get(i).getValue()).intValue());
                for (int i = 0; i < procesos.size(); i++)
                    procesos.get(i).setBoletos(((Number) boletos.get(i).getValue()).intValue());
                for (int i = 0; i < procesos.size(); i++)
                    procesos.get(i).setPrioridad(((Number) prioridades.get(i).getValue()).intValue());
                return;
            } catch (java.text.ParseException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(padre,
                    "Ingrese llegadas entre " + gestor.getReloj() + " y " + maximo + ", y boletos y prioridades enteros positivos.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
