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
import minipc.gui.Componentes.Distintivo;
import minipc.gui.Tema;
import minipc.hardware.CPU;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;

/**
 * PanelDetalleCpu: registros de la CPU elegida y el BCP del proceso que ejecuta, en una
 * grilla compacta (cada dato en una línea; si no cabe, se ve completo al pasar el mouse). Va dentro de la sección desplegable "BCP y registros".
 */
public class PanelDetalleCpu extends JPanel {

    // Campos en el orden en que se muestran: primero la CPU, después el BCP
    private static final String[] CAMPOS = {
        "Proceso", "Estado", "CPU", "PC",
        "IR", "AC", "AX", "BX",
        "CX", "DX", "Flag", "AH/AL",
        "Pila", "Base", "Alcance", "Ráfaga",
        "Llegada", "Inicio", "T. CPU", "Prioridad",
        "BCP en", "Archivos"
    };
    private static final int ESTADO = 1;

    private final JLabel[] valores = new JLabel[CAMPOS.length];
    private final Distintivo distEstado = new Distintivo();

    // E: no aplica
    // S: no aplica (constructor)
    // R: ninguna
    public PanelDetalleCpu(){
        super(new GridLayout(0, 4, 4, 4));
        setOpaque(false);
        for (int i = 0; i < CAMPOS.length; i++) {
            JPanel celda = new JPanel(new BorderLayout(6, 0)) {
                @Override protected void paintComponent(Graphics g){
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xF4F7F5));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            celda.setOpaque(false);
            celda.setBorder(new EmptyBorder(4, 8, 4, 4));
            JLabel nombre = new JLabel(CAMPOS[i]);
            nombre.setFont(Tema.PEQUENA_B);
            nombre.setForeground(i <= 3 ? Tema.VERDE_OSCURO : i <= 11 ? Tema.MORADO : Tema.AZUL);
            if (i != ESTADO) celda.add(nombre, BorderLayout.WEST);   // el estado ya se lee en su distintivo
            if (i == ESTADO) {
                distEstado.setHorizontalAlignment(SwingConstants.LEFT);
                celda.add(distEstado, BorderLayout.CENTER);
            } else {
                JLabel v = new JLabel("-");
                v.setFont(new Font(Tema.MONO_NORMAL.getFamily(), Font.PLAIN, 12));
                v.setForeground(Tema.TEXTO);
                valores[i] = v;
                celda.add(v, BorderLayout.CENTER);
            }
            add(celda);
        }
    }

    private void poner(int i, Object valor){
        String texto = String.valueOf(valor);
        valores[i].setText(texto);
        valores[i].setToolTipText(CAMPOS[i] + ": " + texto);   // por si no cabe completo
    }

    // E: gestor (GestorProcesos) - puede ser null; cpu (CPU) - la CPU elegida, null si no hay gestor
    // S: no aplica (void)
    // R: ninguna
    public void actualizar(GestorProcesos gestor, CPU cpu){
        BCP b = cpu == null ? null : cpu.getBcpActual();
        if (b == null) {
            for (int i = 0; i < valores.length; i++) if (valores[i] != null) poner(i, "-");
            distEstado.setColores(cpu == null ? "-" : "CPU ociosa", Tema.GRIS, Tema.GRIS_CLARO);
            return;
        }
        boolean paginada = gestor.getGestorMemoria().getEstrategia().esPaginada();
        poner(0, "P" + b.getIdProceso());
        distEstado.setEstado(b.getEstado());
        poner(2, "CPU " + b.getCpuId());
        poner(3, cpu.getPC() + (paginada ? " lóg." : ""));
        poner(4, cpu.getIR() == null || cpu.getIR().isEmpty() ? "-" : cpu.getIR());
        poner(5, cpu.getAC());
        poner(6, cpu.getAX());
        poner(7, cpu.getBX());
        poner(8, cpu.getCX());
        poner(9, cpu.getDXTexto() != null ? "\"" + cpu.getDXTexto() + "\"" : String.valueOf(cpu.getDX()));
        poner(10, cpu.isFlagIgual() ? "Igual" : "Distinto");
        poner(11, String.format("%02XH/%d", cpu.getAH(), cpu.getAL()));
        poner(12, b.pilaTexto() + (b.getTopePila() >= 0 ? " SP=" + b.getTopePila() : ""));
        poner(13, paginada ? "0 lóg." : String.valueOf(b.getDireccionBase()));
        poner(14, String.valueOf(b.getTamanioProceso()));
        poner(15, String.valueOf(b.getRafaga()));
        poner(16, b.getTiempoLlegada() + " s");
        poner(17, b.getTiempoInicio() >= 0 ? b.getTiempoInicio() + " s" : "-");
        poner(18, b.getTiempoEmpleado() + " s");
        poner(19, b.getPrioridad());
        poner(20, b.getDireccionBCP() >= 0
            ? b.getDireccionBCP() + "–" + (b.getDireccionBCP() + BCP.TAMANO_BLOQUE - 1) : "-");
        poner(21, String.join(", ", b.getArchivosAbiertos()));
    }
}
