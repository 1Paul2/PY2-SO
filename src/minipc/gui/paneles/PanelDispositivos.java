package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import minipc.gui.Componentes;
import minipc.gui.Componentes.Distintivo;
import minipc.gui.Componentes.Tarjeta;
import minipc.gui.Tema;
import minipc.hardware.CPU;
import minipc.hardware.Disco;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;
import minipc.util.ConfiguracionSistema;

/**
 * PanelDispositivos: tarjeta "Dispositivos". Tiene la tabla resumen de las CPU (proceso,
 * estado y PC de cada una) y el estado de las CPU ocupadas, el monitor y el disco.
 * Al elegir una fila de la tabla avisa qué CPU se seleccionó.
 */
public class PanelDispositivos extends Tarjeta {

    private final ConfiguracionSistema configuracion;
    private final DefaultTableModel modeloCpus;
    private final Distintivo dispCPU, dispMonitor, dispDisco;
    private IntConsumer alElegirCpu = i -> { };

    // E: configuracion (ConfiguracionSistema) - da la cantidad de CPU cuando no hay programas
    // S: no aplica (constructor)
    // R: ninguna
    // Correccion - Poll: se quita el dispositivo "Teclado (entrada)" (INT 09H ya no se usa)
    public PanelDispositivos(ConfiguracionSistema configuracion){
        super("Dispositivos", Tema.MORADO);
        this.configuracion = configuracion;
        JPanel filas = new JPanel(new GridLayout(3, 1, 0, 6));
        filas.setOpaque(false);
        dispCPU = new Distintivo();
        dispMonitor = new Distintivo();
        dispDisco = new Distintivo();
        filas.add(filaDispositivo("CPU ocupadas", dispCPU));
        filas.add(filaDispositivo("Monitor", dispMonitor));
        filas.add(filaDispositivo("Disco", dispDisco));
        modeloCpus = new DefaultTableModel(new String[]{"CPU", "Proceso", "Estado", "PC"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        JTable tablaCpus = new JTable(modeloCpus);
        Componentes.estilizarTabla(tablaCpus);
        tablaCpus.setRowHeight(22);
        tablaCpus.getSelectionModel().addListSelectionListener(e -> {
            int fila = tablaCpus.getSelectedRow();
            if (!e.getValueIsAdjusting() && fila >= 0) alElegirCpu.accept(fila);
        });
        JScrollPane resumenCpus = Componentes.scroll(tablaCpus);
        resumenCpus.setPreferredSize(new Dimension(260, 160));
        resumenCpus.setMinimumSize(new Dimension(220, 160));
        getContenido().add(resumenCpus, BorderLayout.CENTER);
        getContenido().add(filas, BorderLayout.SOUTH);
    }

    // E: accion (IntConsumer) - recibe el índice de la CPU elegida en la tabla (0 = CPU 1)
    // S: no aplica (void)
    // R: ninguna
    public void setAlElegirCpu(IntConsumer accion){ this.alElegirCpu = accion; }

    private JPanel filaDispositivo(String nombre, Distintivo estado){
        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(false);
        JLabel lbl = new JLabel(nombre);
        lbl.setFont(Tema.NORMAL);
        lbl.setForeground(Tema.TEXTO);
        fila.add(lbl, BorderLayout.WEST);
        estado.setHorizontalAlignment(SwingConstants.RIGHT);
        estado.setPreferredSize(new Dimension(170, 24));
        fila.add(estado, BorderLayout.EAST);
        return fila;
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: ninguna
    // Correccion - Poll: se quita el estado del dispositivo teclado
    public void actualizar(GestorProcesos gestor){
        int cantidad = gestor == null ? configuracion.getCantidadCpus() : gestor.getCantidadCpus();
        modeloCpus.setRowCount(cantidad);
        for (int i = 0; i < cantidad; i++) {
            CPU cpu = gestor == null ? null : gestor.getCpu(i);
            BCP b = cpu == null ? null : cpu.getBcpActual();
            modeloCpus.setValueAt("CPU " + (i + 1), i, 0);
            modeloCpus.setValueAt(b == null ? "-" : "P" + b.getIdProceso(), i, 1);
            modeloCpus.setValueAt(b == null ? "Ociosa" : "Ejecución", i, 2);
            modeloCpus.setValueAt(b == null ? "-" : cpu.getPC(), i, 3);
        }

        if (gestor == null) {
            dispCPU.setColores("Libre", Tema.GRIS, Tema.GRIS_CLARO);
            dispMonitor.setColores("Listo", Tema.VERDE_OSCURO, Tema.VERDE_CLARO);
            dispDisco.setColores("Sin archivos", Tema.GRIS, Tema.GRIS_CLARO);
            return;
        }
        Disco d = gestor.getDisco();
        int archivos = 0;
        for (int i = 0; i < d.getTamanoIndice(); i++) {
            String v = d.leer(i);
            if (v != null && !v.isEmpty()) archivos++;
        }
        int ocupadas = gestor.getCpusOcupadas();
        dispCPU.setColores(ocupadas + "/" + gestor.getCantidadCpus() + " ejecutando",
            ocupadas > 0 ? Tema.VERDE_OSCURO : Tema.GRIS,
            ocupadas > 0 ? Tema.VERDE_CLARO : Tema.GRIS_CLARO);
        dispMonitor.setColores(gestor.getMonitor().isEmpty() ? "Listo" : "Mostrando salida",
            Tema.VERDE_OSCURO, Tema.VERDE_CLARO);
        dispDisco.setColores(archivos + " archivo(s)", Tema.MORADO, Tema.MORADO_CLARO);
    }
}
