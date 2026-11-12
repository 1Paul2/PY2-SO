package minipc.gui;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import minipc.gui.Componentes.Boton;
import minipc.gui.Componentes.Distintivo;
import minipc.gui.Componentes.Tarjeta;
import minipc.gui.dialogos.DialogoConfiguracion;
import minipc.gui.dialogos.DialogoEstadisticas;
import minipc.gui.dialogos.DialogoLlegadas;
import minipc.gui.paneles.PanelCola;
import minipc.gui.paneles.PanelCpu;
import minipc.gui.paneles.PanelDisco;
import minipc.gui.paneles.PanelDispositivos;
import minipc.gui.paneles.PanelMemoria;
import minipc.gui.paneles.PanelMonitorConsola;
import minipc.gui.paneles.PanelRecursos;
import minipc.gui.paneles.PanelTablaEjecucion;
import minipc.gui.paneles.PanelTablaPaginas;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;
import minipc.modelo.Instruccion;
import minipc.util.ConfiguracionSistema;
import minipc.util.ParserASM;

/**
 * VentanaPrincipal: ventana del Mini PC. Arma el encabezado con los botones, ubica los
 * paneles (paquete minipc.gui.paneles) y reacciona a los botones. Cada panel sabe
 * dibujarse a sí mismo con actualizar(gestor); toda la lógica la tiene GestorProcesos.
 */
public class VentanaPrincipal extends JFrame {

    private final ConfiguracionSistema configuracion = new ConfiguracionSistema();
    private GestorProcesos gestor;

    // Ejecución automática
    private final Timer timerAuto;
    private boolean modoAutomatico = false;

    // Resultados acumulados de todas las ejecuciones (no se borran al limpiar)
    private final List<String[]> ejecucionesAcumuladas = new ArrayList<>();
    private GestorProcesos gestorRegistrado;   // evita registrar la misma ejecución dos veces

    // ---- Encabezado y barra de herramientas ----
    private JLabel lblReloj;
    private Boton btnCargar, btnEjecutar, btnPaso, btnDetener, btnLlegadas, btnLimpiar, btnEstadisticas, btnConfigurar, btnSalir;
    private JComboBox<String> cmbVelocidad;

    // ---- Paneles ----
    private PanelCola panelCola;
    private PanelRecursos panelRecursos;
    private PanelDispositivos panelDispositivos;
    private Tarjeta tarjetaMemoria;
    private PanelMemoria panelMemoria;
    private PanelTablaPaginas panelPaginas;
    private PanelTablaEjecucion panelEjecucion;
    private PanelMonitorConsola panelMonitorConsola;
    private PanelDisco panelDisco;
    private PanelCpu panelCpu;

    // Correccion - Poll: se quita el timer que contaba los segundos de espera del teclado, y el
    // shutdown hook que devolvía config.txt a los valores por defecto al cerrar
    // E: no aplica
    // S: no aplica constructor
    // R: arma la ventana completa con la configuración de config.txt
    public VentanaPrincipal(){
        super("Mini PC · Gestor de Procesos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Tema.FONDO);
        setLayout(new BorderLayout(0, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 16, 14, 16));

        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearCuerpo(), BorderLayout.CENTER);

        timerAuto = new Timer(400, e -> pasoAutomatico());

        setMinimumSize(new Dimension(1280, 780));
        setSize(new Dimension(1500, 900));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        actualizarVista();
    }


    //ENCABEZADO


    // Correccion - Poll: se quita el chip de hora real; el tiempo se muestra solo en segundos desde 0
    private JPanel crearEncabezado(){
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // ---- Título ----
        JPanel fila1 = new JPanel(new BorderLayout());
        fila1.setOpaque(false);

        JPanel titulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titulo.setOpaque(false);
        JLabel logo = new JLabel("SO", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Tema.VERDE, getWidth(), getHeight(), Tema.VERDE_OSCURO));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logo.setFont(new Font(Tema.TITULO.getFamily(), Font.BOLD, 16));
        logo.setForeground(Color.WHITE);
        logo.setPreferredSize(new Dimension(46, 46));
        titulo.add(logo);

        JPanel textos = new JPanel(new GridLayout(1, 1));
        textos.setOpaque(false);
        JLabel lblTitulo = new JLabel("Mini PC  ·  Gestor de Procesos");
        lblTitulo.setFont(Tema.TITULO);
        lblTitulo.setForeground(Tema.TEXTO);
        textos.add(lblTitulo);
        titulo.add(textos);
        fila1.add(titulo, BorderLayout.WEST);

        // ---- Reloj ----
        JPanel reloj = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        reloj.setOpaque(false);
        lblReloj = chip("Tiempo  0 s", Tema.VERDE_OSCURO, Tema.VERDE_CLARO);
        reloj.add(lblReloj);
        fila1.add(reloj, BorderLayout.EAST);
        panel.add(fila1, BorderLayout.NORTH);

        // ---- Botones ----
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);

        btnCargar       = new Boton("Cargar archivos", Tema.VERDE_OSCURO);
        btnEjecutar     = new Boton("Ejecutar", Tema.VERDE);
        btnPaso         = new Boton("Siguiente", Tema.AZUL);
        btnDetener      = new Boton("Detener", Tema.ROJO);
        btnLlegadas     = new Boton("Llegadas", Tema.AMBAR);
        btnLimpiar      = new Boton("Limpiar", Tema.GRIS);
        btnEstadisticas = new Boton("Estadísticas", Tema.MORADO);
        btnConfigurar   = new Boton("Configuración", new Color(0x334155));
        btnSalir        = new Boton("Salir", new Color(0xB91C1C));
        btnSalir.setToolTipText("Cerrar el Mini PC");

        btnCargar.setToolTipText("Cargar uno o varios archivos .asm");
        btnEjecutar.setToolTipText("Ejecutar automáticamente todos los procesos hasta su finalización");
        btnPaso.setToolTipText("Avanzar un segundo global en todas las CPU");
        btnDetener.setToolTipText("Pausar la ejecución automática");
        btnDetener.setEnabled(false);
        btnLlegadas.setToolTipText("Editar los tiempos de llegada de los procesos que todavía no llegan");

        for (Boton b : new Boton[]{btnCargar, btnEjecutar, btnPaso, btnDetener, btnLlegadas, btnLimpiar, btnEstadisticas, btnConfigurar}) {
            botones.add(b);
        }
        barra.add(botones, BorderLayout.WEST);

        JPanel velocidad = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        velocidad.setOpaque(false);
        JLabel lblVel = new JLabel("Velocidad de Ejecutar:");
        lblVel.setFont(Tema.NORMAL);
        lblVel.setForeground(Tema.TEXTO_SUAVE);
        // Por defecto, "Ejecutar" avanza 1 segundo de CPU por cada segundo real,
        // igual que presionar "Siguiente" una vez por segundo.
        // "Instantánea" corre todo de una vez.
        cmbVelocidad = new JComboBox<>(new String[]{"1 segundo", "Instantánea"});
        cmbVelocidad.setSelectedIndex(0);
        cmbVelocidad.setFont(Tema.NORMAL);
        cmbVelocidad.addActionListener(e -> timerAuto.setDelay(retardoSeleccionado()));
        velocidad.add(lblVel);
        velocidad.add(cmbVelocidad);
        velocidad.add(Box.createHorizontalStrut(8));
        velocidad.add(btnSalir);
        barra.add(velocidad, BorderLayout.EAST);
        panel.add(barra, BorderLayout.SOUTH);

        btnCargar.addActionListener(e -> cargarArchivos());
        btnEjecutar.addActionListener(e -> iniciarAutomatico());
        btnPaso.addActionListener(e -> pasoManual());
        btnDetener.addActionListener(e -> detenerAutomatico(true));
        btnLlegadas.addActionListener(e -> editarLlegadas());
        btnLimpiar.addActionListener(e -> limpiar());
        btnEstadisticas.addActionListener(e -> mostrarEstadisticas());
        btnConfigurar.addActionListener(e -> mostrarConfiguracion());
        btnSalir.addActionListener(e -> salir());
        return panel;
    }

    private JLabel chip(String texto, Color frente, Color fondo){
        Distintivo d = new Distintivo();
        d.setFont(Tema.NEGRITA);
        d.setBorder(new EmptyBorder(6, 14, 6, 14));
        d.setColores(texto, frente, fondo);
        return d;
    }

    //CUERPO


    // E: no aplica
    // S: JPanel - las tres columnas: cola y recursos | memoria, monitor y disco | detalle de CPU
    // R: ninguna
    private JPanel crearCuerpo(){
        panelCola = new PanelCola();
        panelRecursos = new PanelRecursos(configuracion);
        panelDispositivos = new PanelDispositivos(configuracion);
        panelMemoria = new PanelMemoria();
        panelPaginas = new PanelTablaPaginas();
        panelEjecucion = new PanelTablaEjecucion(configuracion);
        panelMonitorConsola = new PanelMonitorConsola();
        panelDisco = new PanelDisco(configuracion);
        panelCpu = new PanelCpu();

        // Elegir una CPU en la tabla de Dispositivos o en el selector muestra su detalle
        panelDispositivos.setAlElegirCpu(panelCpu::elegirCpu);
        panelCpu.setAlCambiarCpu(() -> {
            if (gestor != null) {
                panelCpu.actualizar(gestor);
                panelMemoria.actualizar(gestor, panelCpu.cpuSeleccionada(gestor));
            }
        });

        // Columna izquierda
        JPanel izquierda = Componentes.columna(new Component[]{panelCola, panelRecursos, panelDispositivos}, new double[]{1.0, 0.0, 0.0});

        // Columna central: memoria (con sus pestañas) arriba; monitor y disco abajo
        tarjetaMemoria = new Tarjeta("Memoria principal", Tema.VERDE);
        JTabbedPane vistasMemoria = new JTabbedPane();
        vistasMemoria.addTab("RAM física", panelMemoria);
        vistasMemoria.addTab("Tabla de páginas", panelPaginas);
        vistasMemoria.addTab("Tabla de ejecución", panelEjecucion);
        tarjetaMemoria.getContenido().add(vistasMemoria, BorderLayout.CENTER);
        JPanel abajo = new JPanel(new GridLayout(1, 2, 12, 0));
        abajo.setOpaque(false);
        abajo.add(panelMonitorConsola);
        abajo.add(panelDisco);
        JPanel central = Componentes.columna(new Component[]{tarjetaMemoria, abajo}, new double[]{0.58, 0.42});

        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.insets = new Insets(0, 0, 0, 12);

        g.gridx = 0; g.weightx = 0.27;
        izquierda.setPreferredSize(new Dimension(380, 700));
        izquierda.setMinimumSize(new Dimension(300, 0));
        cuerpo.add(izquierda, g);
        g.gridx = 1; g.weightx = 0.45;
        central.setPreferredSize(new Dimension(650, 700));
        central.setMinimumSize(new Dimension(400, 0));
        cuerpo.add(central, g);
        g.gridx = 2; g.weightx = 0.28; g.insets = new Insets(0, 0, 0, 0);
        panelCpu.setPreferredSize(new Dimension(400, 700));
        panelCpu.setMinimumSize(new Dimension(320, 0));
        cuerpo.add(panelCpu, g);
        return cuerpo;
    }


    // ACCIONES


    private void asegurarGestor(){
        if (gestor == null) {
            gestor = new GestorProcesos(configuracion);
        }
    }

    // E: no aplica
    // S: no aplica
    // R: carga uno o varios .asm; los inválidos se reportan con su línea y motivo
    private void cargarArchivos(){
        JFileChooser selector = new JFileChooser(new File("."));
        selector.setMultiSelectionEnabled(true);
        selector.setFileFilter(new FileNameExtensionFilter("Programas ensamblador (*.asm)", "asm"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        detenerAutomatico(false);
        asegurarGestor();
        List<String> errores = new ArrayList<>();
        int cargados = 0;
        for (File archivo : selector.getSelectedFiles()) {
            try {
                List<Instruccion> instrucciones = ParserASM.leerArchivo(archivo);
                BCP bcp = gestor.cargarPrograma(instrucciones, archivo.getName());
                gestor.imprimir("[SO] " + archivo.getName() + " cargado como P"
                    + bcp.getIdProceso() + " (" + instrucciones.size() + " instrucciones)");
                cargados++;
            } catch (Exception e) {
                errores.add("• " + e.getMessage());
            }
        }
        if (cargados > 0) DialogoLlegadas.mostrar(this, gestor);
        actualizarVista();

        if (!errores.isEmpty()) {
            JTextArea txt = new JTextArea(String.join("\n", errores));
            txt.setEditable(false);
            txt.setFont(Tema.MONO_NORMAL);
            txt.setForeground(Tema.ROJO);
            txt.setBackground(Tema.ROJO_CLARO);
            txt.setBorder(new EmptyBorder(8, 8, 8, 8));
            txt.setLineWrap(true);
            txt.setWrapStyleWord(true);
            JScrollPane sp = new JScrollPane(txt);
            sp.setPreferredSize(new Dimension(560, Math.min(260, 60 + errores.size() * 40)));
            JOptionPane.showMessageDialog(this, sp,
                cargados + " cargado(s), " + errores.size() + " con errores", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Botón "Llegadas": edita las llegadas de los procesos que todavía no llegan
    private void editarLlegadas(){
        if (gestor == null || gestor.getPendientesLlegada().isEmpty()) {
            avisar("No hay procesos pendientes de llegar.");
            return;
        }
        DialogoLlegadas.mostrar(this, gestor);
        actualizarVista();
    }

    // Correccion - Poll: ya no se bloquea ni arranca el conteo de espera del teclado
    // Botón "Siguiente": 1 segundo de CPU
    private void pasoManual(){
        if (!validarHayProcesos()) return;
        gestor.ejecutarUnPaso();
        actualizarVista();
        if (!gestor.hayProcesos()) {
            finEjecucion();
        }
    }

    // Correccion - Poll: se quita el aviso de "un proceso espera un valor de teclado"
    // Botón "Ejecutar": modo automático
    private void iniciarAutomatico(){
        if (!validarHayProcesos()) return;
        if (cmbVelocidad.getSelectedIndex() == 1) {          // instantánea
            gestor.ejecutarTodo();
            actualizarVista();
            if (!gestor.hayProcesos()) {
                finEjecucion();
            }
            return;
        }
        modoAutomatico = true;
        timerAuto.setDelay(retardoSeleccionado());
        timerAuto.start();
        actualizarBotones();
    }

    // Correccion - Poll: el timer ya no se detiene esperando el teclado
    private void pasoAutomatico(){
        if (gestor == null || !gestor.hayProcesos()) {
            detenerAutomatico(false);
            actualizarVista();
            if (gestor != null) finEjecucion();
            return;
        }
        gestor.ejecutarUnPaso();
        actualizarVista();
    }

    private void detenerAutomatico(boolean manual){
        modoAutomatico = false;
        timerAuto.stop();
        if (manual && gestor != null) gestor.imprimir("[SO] Ejecución automática pausada");
        actualizarBotones();
        actualizarVista();
    }

    private int retardoSeleccionado(){
        return 1000;
    }

    private void finEjecucion(){
        if (gestorRegistrado != gestor) {
            ejecucionesAcumuladas.add(gestor.getResumen());
            gestorRegistrado = gestor;
        }
        detenerAutomatico(false);
        gestor.imprimir("[SO] Todos los procesos finalizaron en " + gestor.getReloj() + " s");
        actualizarVista();
        int r = JOptionPane.showConfirmDialog(this, "Todos los procesos finalizaron.\n¿Ver estadísticas?",
            "Ejecución completa", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (r == JOptionPane.YES_OPTION) mostrarEstadisticas();
    }

    // E: no aplica
    // S: no aplica (void)
    // R: pide confirmación si hay procesos sin terminar y cierra la aplicación
    private void salir(){
        String mensaje = (gestor != null && gestor.hayProcesos())
            ? "Hay procesos sin terminar. ¿Seguro que querés salir?"
            : "¿Seguro que querés salir del Mini PC?";
        int r = JOptionPane.showConfirmDialog(this, mensaje, "Salir",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            timerAuto.stop();
            dispose();
            System.exit(0);
        }
    }

    // Correccion - Poll: ya no detiene el timer de espera ni limpia el campo del teclado
    // E: no aplica
    // S: no aplica (void)
    // R: siempre pide confirmación antes de borrar procesos, memoria, disco y pantalla;
    //    el acumulado de estadísticas se conserva
    private void limpiar(){
        if (gestor == null) {
            JOptionPane.showMessageDialog(this, "No hay nada que limpiar.", "Limpiar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String mensaje = gestor.hayProcesos()
            ? "Hay procesos sin terminar.\n¿Está seguro de que quiere limpiar todo?"
            : "¿Está seguro de que quiere limpiar todo?";
        int r = JOptionPane.showConfirmDialog(this,
            mensaje + "\nSe borrarán los procesos, la memoria, el disco y la pantalla.\nEl acumulado de estadísticas se conserva.",
            "Confirmar limpieza", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        detenerAutomatico(false);
        gestor = null;
        actualizarVista();
    }

    private boolean validarHayProcesos(){
        if (gestor == null || gestor.getTodosLosProcesos().isEmpty()) {
            avisar("Primero cargá uno o más archivos .asm");
            return false;
        }
        if (!gestor.hayProcesos()) {
            avisar("Todos los procesos ya finalizaron. Usá Limpiar para empezar de nuevo.");
            return false;
        }
        return true;
    }

    private void avisar(String msg){
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    // E: no aplica
    // S: no aplica (void)
    // R: muestra la ejecución actual por proceso y el acumulado de todas las ejecuciones
    private void mostrarEstadisticas(){
        boolean hayActual = gestor != null && !gestor.getTodosLosProcesos().isEmpty();
        if (!hayActual && ejecucionesAcumuladas.isEmpty()) {
            avisar("Todavía no hay procesos ni ejecuciones registradas.");
            return;
        }
        DialogoEstadisticas.mostrar(this, gestor, ejecucionesAcumuladas);
    }

    // E: no aplica
    // S: no aplica (void)
    // R: no se puede cambiar la configuración en medio de una ejecución; al aplicarla se reinicia el sistema
    private void mostrarConfiguracion(){
        if (gestor != null && gestor.getReloj() > 0 && gestor.hayProcesos()) {
            avisar("La cantidad de CPU se configura antes de ejecutar. Usá Limpiar para iniciar otra simulación.");
            return;
        }
        if (!DialogoConfiguracion.mostrar(this, configuracion)) return;
        boolean habiaProcesos = gestor != null;
        gestor = null;
        actualizarVista();
        if (habiaProcesos) {
            JOptionPane.showMessageDialog(this, "Configuración aplicada. Se reinició el sistema: volvé a cargar los programas.");
        }
    }


    // ACTUALIZAR LA VISTA


    // E: no aplica
    // S: no aplica (void)
    // R: refresca todos los paneles con el estado actual del GestorProcesos
    private void actualizarVista(){
        panelCpu.setCantidadCpus(gestor == null ? configuracion.getCantidadCpus() : gestor.getCantidadCpus());
        panelDispositivos.actualizar(gestor);
        actualizarEncabezado();
        panelCola.actualizar(gestor);
        panelRecursos.actualizar(gestor);
        panelCpu.actualizar(gestor);
        tarjetaMemoria.setExtra(gestor == null ? configuracion.getEstrategiaMemoria().toString()
            : gestor.getGestorMemoria().getEstrategia() + " · fallos " + gestor.getGestorMemoria().getFallosPagina());
        panelMemoria.actualizar(gestor, panelCpu.cpuSeleccionada(gestor));
        panelPaginas.actualizar(gestor);
        panelDisco.actualizar(gestor);
        panelMonitorConsola.actualizar(gestor);
        panelEjecucion.actualizar(gestor);
        actualizarBotones();
    }

    private void actualizarEncabezado(){
        String texto = "Tiempo  " + (gestor == null ? 0 : gestor.getReloj()) + " s";
        ((Distintivo) lblReloj).setColores(texto, Tema.VERDE_OSCURO, Tema.VERDE_CLARO);
    }

    private void actualizarBotones(){
        boolean corriendo = modoAutomatico;
        btnDetener.setEnabled(corriendo);
        btnEjecutar.setEnabled(!corriendo);
        btnPaso.setEnabled(!corriendo);
        btnCargar.setEnabled(!corriendo);
        btnLlegadas.setEnabled(!corriendo);
        btnConfigurar.setEnabled(!corriendo);
    }
}
