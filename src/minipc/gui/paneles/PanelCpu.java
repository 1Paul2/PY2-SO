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
import minipc.gui.Componentes;
import minipc.gui.Componentes.Distintivo;
import minipc.gui.Componentes.Tarjeta;
import minipc.gui.Tema;
import minipc.hardware.CPU;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;

/**
 * PanelCpu: columna derecha. Tiene el selector de CPU y tres tarjetas de esa CPU:
 * sus registros, la pila del proceso que ejecuta y su BCP.
 */
public class PanelCpu extends JPanel {

    private final JComboBox<String> cmbCpuDetalle;
    private boolean reconstruyendo = false;   // evita avisar mientras se rehace la lista de CPU
    private Runnable alCambiarCpu = () -> { };

    private final JLabel lblProcesoActual, lblInstruccion;
    private final Distintivo distEstadoCPU;
    private final JLabel[] valoresRegistro = new JLabel[10];  // PC, IR, AC, AX, BX, CX, DX, FLAG, AH, AL
    private final PanelPila panelPila;
    private final JLabel[] valoresBCP = new JLabel[12];
    private Distintivo distEstadoBCP;

    // E: no aplica
    // S: no aplica (constructor)
    // R: ninguna
    // Correccion - Poll: se quita la barra de progreso de la instrucción según su peso
    public PanelCpu(){
        super(new BorderLayout());
        setOpaque(false);

        // CPU
        Tarjeta cpu = new Tarjeta("Detalle de CPU", Tema.VERDE);
        JPanel contCPU = new JPanel(new BorderLayout(0, 10));
        contCPU.setOpaque(false);

        JPanel filaProceso = new JPanel(new BorderLayout());
        filaProceso.setOpaque(false);
        lblProcesoActual = new JLabel("CPU libre");
        lblProcesoActual.setFont(Tema.NEGRITA);
        lblProcesoActual.setForeground(Tema.TEXTO);
        distEstadoCPU = new Distintivo();
        distEstadoCPU.setPreferredSize(new Dimension(110, 24));
        filaProceso.add(lblProcesoActual, BorderLayout.CENTER);
        filaProceso.add(distEstadoCPU, BorderLayout.EAST);
        cmbCpuDetalle = new JComboBox<>();
        cmbCpuDetalle.setFont(Tema.NORMAL);
        cmbCpuDetalle.setToolTipText("CPU cuyos registros, pila y BCP se muestran abajo");
        cmbCpuDetalle.addActionListener(e -> { if (!reconstruyendo) alCambiarCpu.run(); });
        JPanel cabeceraCpu = new JPanel(new BorderLayout(0, 6));
        cabeceraCpu.setOpaque(false);
        cabeceraCpu.add(cmbCpuDetalle, BorderLayout.NORTH);
        cabeceraCpu.add(filaProceso, BorderLayout.SOUTH);
        contCPU.add(cabeceraCpu, BorderLayout.NORTH);

        JPanel registros = new JPanel(new GridLayout(5, 2, 8, 6));
        registros.setOpaque(false);
        String[] nombres = {"PC", "IR", "AC", "AX", "BX", "CX", "DX", "Flag CMP", "AH (INT 21H)", "AL (INT 21H)"};
        Color[] colores = {Tema.ROJO, Tema.VERDE_OSCURO, Tema.AZUL, Tema.MORADO,
                           Tema.MORADO, Tema.MORADO, Tema.MORADO, Tema.AMBAR,
                           new Color(0x0F766E), new Color(0x0F766E)};
        for (int i = 0; i < nombres.length; i++) {
            registros.add(crearRegistro(nombres[i], colores[i], i));
        }
        contCPU.add(registros, BorderLayout.CENTER);

        JPanel instr = new JPanel(new BorderLayout(0, 4));
        instr.setOpaque(false);
        lblInstruccion = new JLabel("Instrucción en curso: -");
        lblInstruccion.setFont(Tema.PEQUENA);
        lblInstruccion.setForeground(Tema.TEXTO_SUAVE);
        instr.add(lblInstruccion, BorderLayout.NORTH);
        contCPU.add(instr, BorderLayout.SOUTH);
        cpu.getContenido().add(contCPU, BorderLayout.CENTER);

        // Pila
        Tarjeta pila = new Tarjeta("Pila del proceso (tamaño 5)", Tema.AMBAR);
        panelPila = new PanelPila();
        pila.getContenido().add(panelPila, BorderLayout.CENTER);

        // BCP
        Tarjeta bcp = new Tarjeta("BCP actual", Tema.AZUL);
        JPanel datos = new JPanel(new GridBagLayout());
        datos.setOpaque(false);
        String[] campos = {"Proceso", "Estado", "Base", "Alcance", "Prioridad", "CPU",
                           "Inicio", "Tiempo empleado", "Dirección del BCP", "Archivos abiertos", "Ráfaga", "Llegada"};
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2, 0, 2, 10);
        for (int i = 0; i < campos.length; i++) {
            g.gridy = i;
            g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
            JLabel lbl = new JLabel(campos[i]);
            lbl.setFont(Tema.NORMAL);
            lbl.setForeground(Tema.TEXTO_SUAVE);
            datos.add(lbl, g);
            g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            if (i == 1) {
                distEstadoBCP = new Distintivo();
                distEstadoBCP.setHorizontalAlignment(SwingConstants.LEFT);
                distEstadoBCP.setPreferredSize(new Dimension(110, 22));
                datos.add(distEstadoBCP, g);
            } else {
                JLabel v = new JLabel("-");
                v.setFont(Tema.MONO_NORMAL);
                v.setForeground(Tema.TEXTO);
                valoresBCP[i] = v;
                datos.add(v, g);
            }
        }
        bcp.getContenido().add(datos, BorderLayout.NORTH);

        JScrollPane scroll = Componentes.scroll(Componentes.columna(new Component[]{cpu, pila, bcp}, new double[]{0.0, 0.0, 1.0}));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel crearRegistro(String nombre, Color color, int indice){
        JPanel p = new JPanel(new BorderLayout(0, 2)) {
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.aclarar(color, 0.9f));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(4, 12, 4, 8));
        JLabel lbl = new JLabel(nombre);
        lbl.setFont(Tema.PEQUENA_B);
        lbl.setForeground(color);
        JLabel valor = new JLabel("0");
        valor.setFont(indice == 1 ? Tema.MONO_NORMAL : Tema.MONO_GRANDE);
        valor.setForeground(Tema.TEXTO);
        valoresRegistro[indice] = valor;
        p.add(lbl, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------ Selección de CPU ------------------------------

    // E: accion (Runnable) - se ejecuta cuando el usuario elige otra CPU en el selector
    // S: no aplica (void)
    // R: ninguna
    public void setAlCambiarCpu(Runnable accion){ this.alCambiarCpu = accion; }

    // E: cantidad (int) - cantidad de CPU del simulador
    // S: no aplica (void)
    // R: solo rehace la lista si cambió la cantidad; vuelve a CPU 1 sin avisar del cambio
    public void setCantidadCpus(int cantidad){
        if (cmbCpuDetalle.getItemCount() == cantidad) return;
        reconstruyendo = true;
        cmbCpuDetalle.removeAllItems();
        for (int i = 1; i <= cantidad; i++) cmbCpuDetalle.addItem("CPU " + i);
        cmbCpuDetalle.setSelectedIndex(0);
        reconstruyendo = false;
    }

    // E: indice (int) - CPU a mostrar (0 = CPU 1)
    // S: no aplica (void)
    // R: se ignora si el índice no existe
    public void elegirCpu(int indice){
        if (indice >= 0 && indice < cmbCpuDetalle.getItemCount()) cmbCpuDetalle.setSelectedIndex(indice);
    }

    // E: gestor (GestorProcesos) - puede ser null
    // S: CPU - la CPU elegida en el selector, o null si no hay gestor
    // R: ninguna
    // Correccion - Poll: se quita la hora real (HH:mm:ss); queda solo el tiempo en segundos desde 0
    public CPU cpuSeleccionada(GestorProcesos gestor){
        if (gestor == null) return null;
        int indice = Math.max(0, cmbCpuDetalle.getSelectedIndex());
        return gestor.getCpu(Math.min(indice, gestor.getCantidadCpus() - 1));
    }

    // ------------------------------ Actualización ------------------------------

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: refresca registros, pila y BCP de la CPU elegida
    public void actualizar(GestorProcesos gestor){
        actualizarRegistros(gestor);
        actualizarBCP(gestor);
    }

    // Correccion - Poll: ya no muestra "N de M s" según el peso; cada instrucción dura 1 s.
    // Tampoco muestra "desde memoria virtual" (memoria virtual partida)
    private void actualizarRegistros(GestorProcesos gestor){
        CPU cpu = cpuSeleccionada(gestor);
        BCP actual = cpu == null ? null : cpu.getBcpActual();

        if (actual == null) {
            lblProcesoActual.setText("CPU libre");
            lblProcesoActual.setIcon(new Componentes.IconoPunto(Tema.GRIS, 10));
            distEstadoCPU.setColores("Ociosa", Tema.GRIS, Tema.GRIS_CLARO);
        } else {
            lblProcesoActual.setText("Proceso P" + actual.getIdProceso() + "  ·  "
                + actual.getArchivosAbiertos().get(0));
            lblProcesoActual.setIcon(new Componentes.IconoPunto(Tema.colorProceso(actual.getIdProceso()), 10));
            distEstadoCPU.setEstado(actual.getEstado());
        }

        if (cpu == null) {
            for (int i = 0; i < valoresRegistro.length; i++) valoresRegistro[i].setText(i == 1 ? "-" : "0");
            valoresRegistro[7].setText("-");
            valoresRegistro[6].setFont(Tema.MONO_GRANDE);
            lblInstruccion.setText("Instrucción en curso: -");
            return;
        }
        valoresRegistro[0].setText(String.valueOf(cpu.getPC()));
        valoresRegistro[0].setToolTipText(gestor.getGestorMemoria().getEstrategia().esPaginada()
            ? "PC lógico. La tabla de páginas traduce a RAM o disco." : "Dirección física de la siguiente instrucción");
        valoresRegistro[1].setText(cpu.getIR() == null || cpu.getIR().isEmpty() ? "-" : cpu.getIR());
        valoresRegistro[2].setText(String.valueOf(cpu.getAC()));
        valoresRegistro[3].setText(String.valueOf(cpu.getAX()));
        valoresRegistro[4].setText(String.valueOf(cpu.getBX()));
        valoresRegistro[5].setText(String.valueOf(cpu.getCX()));
        if (cpu.getDXTexto() != null) {
            valoresRegistro[6].setText("\"" + cpu.getDXTexto() + "\"");    // nombre de archivo
            valoresRegistro[6].setFont(Tema.MONO_NORMAL);
        } else {
            valoresRegistro[6].setText(String.valueOf(cpu.getDX()));
            valoresRegistro[6].setFont(Tema.MONO_GRANDE);
        }
        valoresRegistro[7].setText(cpu.isFlagIgual() ? "Igual" : "Distinto");
        valoresRegistro[8].setText(String.format("%d (%02XH)", cpu.getAH(), cpu.getAH()));
        valoresRegistro[9].setText(String.valueOf(cpu.getAL()));

        if (actual != null && cpu.getIR() != null && !cpu.getIR().isEmpty()) {
            lblInstruccion.setText("Última instrucción: " + cpu.getIR() + "  ·  1 s");
        } else {
            lblInstruccion.setText(actual == null ? "Instrucción en curso: -" : "Lista para la primera instrucción");
        }
    }

    // Correccion - Poll: el tiempo de inicio se muestra en segundos desde 0, no en hora real,
    // y el alcance ya no se divide en "RAM + virtual"
    // Correccion - Poll (observación PY1): la dirección del BCP muestra el bloque completo que ocupa
    private void actualizarBCP(GestorProcesos gestor){
        BCP b = (gestor == null) ? null : cpuSeleccionada(gestor).getBcpActual();
        panelPila.setBCP(b);
        if (b == null) {
            for (JLabel l : valoresBCP) if (l != null) l.setText("-");
            distEstadoBCP.setColores("-", Tema.GRIS, Tema.GRIS_CLARO);
            return;
        }
        valoresBCP[0].setText("P" + b.getIdProceso());
        distEstadoBCP.setEstado(b.getEstado());
        valoresBCP[2].setText(gestor.getGestorMemoria().getEstrategia().esPaginada()
            ? "0 (lógica)" : String.valueOf(b.getDireccionBase()));
        valoresBCP[3].setText(b.getTamanioProceso() + " posiciones");
        valoresBCP[4].setText(String.valueOf(b.getPrioridad()));
        valoresBCP[5].setText("CPU " + b.getCpuId());
        valoresBCP[6].setText(b.getTiempoInicio() >= 0 ? "t = " + b.getTiempoInicio() + " s" : "-");
        valoresBCP[7].setText(b.getTiempoEmpleado() + " s");
        valoresBCP[8].setText(b.getDireccionBCP() >= 0
            ? "Memoria[" + b.getDireccionBCP() + "–" + (b.getDireccionBCP() + BCP.TAMANO_BLOQUE - 1) + "]" : "-");
        valoresBCP[9].setText(String.join(", ", b.getArchivosAbiertos()));
        valoresBCP[10].setText(b.getRafaga() + " líneas");
        valoresBCP[11].setText("t = " + b.getTiempoLlegada() + " s");
    }
}
