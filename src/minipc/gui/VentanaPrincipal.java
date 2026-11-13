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
import minipc.gui.Componentes.Desplegable;
import minipc.gui.Componentes.Distintivo;
import minipc.gui.Componentes.Tarjeta;
import minipc.gui.dialogos.DialogoConfiguracion;
import minipc.gui.dialogos.DialogoEstadisticas;
import minipc.gui.dialogos.DialogoLlegadas;
import minipc.gui.paneles.PanelCola;
import minipc.gui.paneles.PanelConsola;
import minipc.gui.paneles.PanelDetalleCpu;
import minipc.gui.paneles.PanelDisco;
import minipc.gui.paneles.PanelMemoria;
import minipc.gui.paneles.PanelMonitor;
import minipc.gui.paneles.PanelRecursos;
import minipc.gui.paneles.PanelTablaEjecucion;
import minipc.gui.paneles.PanelTablaPaginas;
import minipc.gui.paneles.PanelTarjetasCpu;
import minipc.hardware.CPU;
import minipc.hardware.EstrategiaMemoria;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;
import minipc.modelo.Instruccion;
import minipc.util.ConfiguracionSistema;
import minipc.util.ParserASM;

/**
 * VentanaPrincipal: ventana del Mini PC. Arriba el encabezado con la configuración activa y
 * los botones agrupados por paso (preparar, ejecutar, resultados); debajo una tarjeta por CPU;
 * a la izquierda la cola de trabajos con los recursos y el BCP desplegables; al centro las
 * pestañas grandes (tabla de ejecución, memoria, páginas, disco, monitor y consola).
 * Cada panel se dibuja con actualizar(gestor); toda la lógica la tiene GestorProcesos.
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
    private Distintivo chipCpus, chipAlgoritmo, chipMemoria;
    private Boton btnCargar, btnEjecutar, btnPaso, btnDetener, btnLlegadas, btnLimpiar, btnEstadisticas, btnConfigurar, btnSalir;
    private JComboBox<String> cmbVelocidad;

    // ---- Paneles ----
    private PanelTarjetasCpu panelTarjetas;
    private PanelCola panelCola;
    private PanelRecursos panelRecursos;
    private PanelDetalleCpu panelDetalle;
    private Desplegable despRecursos, despDetalle;
    private Tarjeta tarjetaVistas;
    private JTabbedPane vistas;
    private PanelTablaEjecucion panelEjecucion;
    private PanelMemoria panelMemoria;
    private PanelTablaPaginas panelPaginas;
    private PanelDisco panelDisco;
    private PanelMonitor panelMonitor;
    private PanelConsola panelConsola;

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


    // E: no aplica
    // S: JPanel - título con la configuración activa y el reloj; abajo los botones por paso
    // R: ninguna
    private JPanel crearEncabezado(){
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // ---- Título, configuración activa y reloj ----
        JPanel fila1 = new JPanel(new BorderLayout());
        fila1.setOpaque(false);
        JPanel titulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
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
        logo.setPreferredSize(new Dimension(44, 44));
        titulo.add(logo);
        JLabel lblTitulo = new JLabel("Mini PC  ·  Gestor de Procesos");
        lblTitulo.setFont(Tema.TITULO);
        lblTitulo.setForeground(Tema.TEXTO);
        titulo.add(lblTitulo);
        titulo.add(Box.createHorizontalStrut(6));
        chipCpus = chip("", Tema.TEXTO, Color.WHITE);
        chipAlgoritmo = chip("", Tema.TEXTO, Color.WHITE);
        chipMemoria = chip("", Tema.TEXTO, Color.WHITE);
        chipCpus.setToolTipText("Cantidad de CPU (se cambia en Configuración)");
        chipAlgoritmo.setToolTipText("Algoritmo de planificación y sus parámetros");
        chipMemoria.setToolTipText("Estrategia de asignación de memoria");
        titulo.add(chipCpus);
        titulo.add(chipAlgoritmo);
        titulo.add(chipMemoria);
        fila1.add(titulo, BorderLayout.WEST);
        lblReloj = chip("Tiempo  0 s", Tema.VERDE_OSCURO, Tema.VERDE_CLARO);
        lblReloj.setFont(new Font(Tema.TITULO.getFamily(), Font.BOLD, 15));
        JPanel reloj = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        reloj.setOpaque(false);
        reloj.add(lblReloj);
        fila1.add(reloj, BorderLayout.EAST);
        panel.add(fila1, BorderLayout.NORTH);

        // ---- Botones agrupados en el orden de uso ----
        btnConfigurar   = new Boton("Configuración", new Color(0x334155));
        btnCargar       = new Boton("Cargar .asm", Tema.VERDE_OSCURO);
        btnLlegadas     = new Boton("Llegadas", Tema.AMBAR);
        btnEjecutar     = new Boton("▶  Ejecutar", Tema.VERDE);
        btnPaso         = new Boton("Siguiente ›", Tema.AZUL);
        btnDetener      = new Boton("■  Detener", Tema.ROJO);
        btnEstadisticas = new Boton("Estadísticas", Tema.MORADO);
        btnLimpiar      = new Boton("Limpiar", Tema.GRIS);
        btnSalir        = new Boton("Salir", new Color(0xB91C1C));

        btnConfigurar.setToolTipText("CPU, algoritmo con sus parámetros, tamaños de memoria y estrategia de asignación");
        btnCargar.setToolTipText("Cargar uno o varios archivos .asm");
        btnLlegadas.setToolTipText("Editar los tiempos de llegada de los procesos que todavía no llegan");
        btnEjecutar.setToolTipText("Ejecutar automáticamente todos los procesos hasta su finalización");
        btnPaso.setToolTipText("Avanzar un segundo global en todas las CPU");
        btnDetener.setToolTipText("Pausar la ejecución automática");
        btnDetener.setEnabled(false);
        btnEstadisticas.setToolTipText("Estadísticas de esta ejecución y el acumulado de todas");
        btnLimpiar.setToolTipText("Borrar los procesos para empezar otra ejecución (el acumulado se conserva)");
        btnSalir.setToolTipText("Cerrar el Mini PC");
        for (Boton b : new Boton[]{btnConfigurar, btnCargar, btnLlegadas, btnEjecutar, btnPaso, btnDetener, btnEstadisticas, btnLimpiar}) {
            b.setBorder(new EmptyBorder(8, 12, 8, 12));   // un poco más compactos para que entren en 1280 px
        }

        // Por defecto, "Ejecutar" avanza 1 segundo de CPU por cada segundo real,
        // igual que presionar "Siguiente" una vez por segundo. "Instantánea" corre todo de una vez.
        cmbVelocidad = new JComboBox<>(new String[]{"1 segundo", "Instantánea"});
        cmbVelocidad.setSelectedIndex(0);
        cmbVelocidad.setFont(Tema.NORMAL);
        cmbVelocidad.setToolTipText("Velocidad del botón Ejecutar");
        cmbVelocidad.addActionListener(e -> timerAuto.setDelay(retardoSeleccionado()));
        cmbVelocidad.setPreferredSize(new Dimension(112, 32));

        JPanel barra = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.TARJETA);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(Tema.BORDE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        barra.setOpaque(false);
        barra.setBorder(new EmptyBorder(8, 12, 8, 12));
        JPanel pasos = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pasos.setOpaque(false);
        pasos.add(grupo("1 · PREPARAR"));
        pasos.add(btnConfigurar);
        pasos.add(btnCargar);
        pasos.add(btnLlegadas);
        pasos.add(separador());
        pasos.add(grupo("2 · EJECUTAR"));
        pasos.add(btnEjecutar);
        pasos.add(btnPaso);
        pasos.add(btnDetener);
        pasos.add(cmbVelocidad);
        pasos.add(separador());
        pasos.add(grupo("3 · RESULTADOS"));
        pasos.add(btnEstadisticas);
        pasos.add(btnLimpiar);
        barra.add(pasos, BorderLayout.CENTER);
        panel.add(barra, BorderLayout.SOUTH);
        reloj.add(btnSalir);

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

    private Distintivo chip(String texto, Color frente, Color fondo){
        Distintivo d = new Distintivo();
        d.setFont(Tema.NEGRITA);
        d.setBorder(new EmptyBorder(6, 14, 6, 14));
        d.setColores(texto, frente, fondo);
        return d;
    }

    private JLabel grupo(String texto){
        JLabel l = new JLabel(texto);
        l.setFont(Tema.PEQUENA_B);
        l.setForeground(Tema.TEXTO_SUAVE);
        l.setBorder(new EmptyBorder(0, 4, 0, 2));
        return l;
    }

    private JComponent separador(){
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(10, 30));
        s.setForeground(Tema.BORDE);
        return s;
    }

    //CUERPO


    // E: no aplica
    // S: JPanel - tarjetas de CPU arriba; abajo la cola (izquierda) y las pestañas (centro)
    // R: ninguna
    private JPanel crearCuerpo(){
        panelTarjetas = new PanelTarjetasCpu(configuracion);
        panelTarjetas.setPreferredSize(new Dimension(1000, 108));
        // Clic en una tarjeta: se abre el BCP de esa CPU
        panelTarjetas.setAlElegirCpu(i -> { despDetalle.setAbierto(true); actualizarVista(); });

        // Columna izquierda: cola con toda la altura; recursos y BCP desplegables
        panelCola = new PanelCola();
        panelRecursos = new PanelRecursos(configuracion);
        panelDetalle = new PanelDetalleCpu();
        despRecursos = new Desplegable("Recursos del sistema", Tema.AZUL, panelRecursos, false);
        despDetalle = new Desplegable("BCP y registros", Tema.MORADO, panelDetalle, false);
        JPanel izquierda = Componentes.columna(new Component[]{panelCola, despRecursos, despDetalle}, new double[]{1.0, 0.0, 0.0});
        izquierda.setPreferredSize(new Dimension(430, 600));
        izquierda.setMinimumSize(new Dimension(380, 0));

        // Centro: pestañas grandes
        panelEjecucion = new PanelTablaEjecucion(configuracion);
        panelMemoria = new PanelMemoria();
        panelPaginas = new PanelTablaPaginas();
        panelDisco = new PanelDisco(configuracion);
        panelMonitor = new PanelMonitor();
        panelConsola = new PanelConsola();
        vistas = new JTabbedPane();
        vistas.setFont(Tema.NEGRITA);
        vistas.addTab("Tabla de ejecución", panelEjecucion);
        vistas.addTab("Memoria (RAM)", panelMemoria);
        vistas.addTab("Tabla de páginas", panelPaginas);
        vistas.addTab("Disco", panelDisco);
        vistas.addTab("Monitor", panelMonitor);
        vistas.addTab("Consola del S.O.", panelConsola);
        vistas.setToolTipTextAt(0, "Qué proceso ejecutó cada núcleo en cada segundo");
        vistas.setToolTipTextAt(1, "Dónde está cada proceso en la memoria principal");
        vistas.setToolTipTextAt(3, "Índice de archivos, archivos y memoria virtual");
        vistas.setToolTipTextAt(4, "Lo que imprimen los programas con INT 10H");
        vistas.setToolTipTextAt(5, "Lo que hace el sistema operativo: despachador, memoria, archivos y errores");
        tarjetaVistas = new Tarjeta("Memoria y ejecución", Tema.VERDE);
        tarjetaVistas.getContenido().add(vistas, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout(12, 0));
        abajo.setOpaque(false);
        abajo.add(izquierda, BorderLayout.WEST);
        abajo.add(tarjetaVistas, BorderLayout.CENTER);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        cuerpo.add(panelTarjetas, BorderLayout.NORTH);
        cuerpo.add(abajo, BorderLayout.CENTER);
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

    // Botón "Siguiente": 1 segundo de CPU
    private void pasoManual(){
        if (!validarHayProcesos()) return;
        gestor.ejecutarUnPaso();
        actualizarVista();
        if (!gestor.hayProcesos()) {
            finEjecucion();
        }
    }

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
        panelTarjetas.actualizar(gestor);
        CPU cpuElegida = gestor == null ? null
            : gestor.getCpu(Math.min(panelTarjetas.getElegida(), gestor.getCantidadCpus() - 1));
        actualizarEncabezado();
        panelCola.actualizar(gestor);
        panelRecursos.actualizar(gestor);
        panelDetalle.actualizar(gestor, cpuElegida);
        BCP enCpu = cpuElegida == null ? null : cpuElegida.getBcpActual();
        despDetalle.setTitulo("BCP y registros · CPU " + (panelTarjetas.getElegida() + 1)
            + (enCpu == null ? "" : " · P" + enCpu.getIdProceso()));
        EstrategiaMemoria estrategia = gestor == null ? configuracion.getEstrategiaMemoria()
            : gestor.getGestorMemoria().getEstrategia();
        tarjetaVistas.setExtra(estrategia + (gestor == null ? "" : " · fallos de página " + gestor.getGestorMemoria().getFallosPagina()));
        panelEjecucion.actualizar(gestor);
        panelMemoria.actualizar(gestor, cpuElegida);
        panelPaginas.actualizar(gestor);
        panelDisco.actualizar(gestor);
        panelMonitor.actualizar(gestor);
        panelConsola.actualizar(gestor);
        // La tabla de páginas solo tiene sentido con paginación
        vistas.setEnabledAt(2, estrategia.esPaginada());
        vistas.setToolTipTextAt(2, estrategia.esPaginada() ? "Marco de RAM o posición en disco de cada página"
            : "Solo se usa con paginación física o virtual");
        if (vistas.getSelectedIndex() == 2 && !estrategia.esPaginada()) vistas.setSelectedIndex(1);
        actualizarBotones();
    }

    // E: no aplica
    // S: no aplica (void)
    // R: el reloj y los chips con la configuración activa
    private void actualizarEncabezado(){
        ((Distintivo) lblReloj).setColores("Tiempo  " + (gestor == null ? 0 : gestor.getReloj()) + " s",
            Tema.VERDE_OSCURO, Tema.VERDE_CLARO);
        int cpus = gestor == null ? configuracion.getCantidadCpus() : gestor.getCantidadCpus();
        chipCpus.setColores(cpus + (cpus == 1 ? " CPU" : " CPU"), Tema.TEXTO, Color.WHITE);
        chipAlgoritmo.setColores(textoAlgoritmo(), Tema.TEXTO, Color.WHITE);
        chipMemoria.setColores(textoMemoria(), Tema.TEXTO, Color.WHITE);
    }

    // S: String - el algoritmo de CPU con sus parámetros, p. ej. "Round Robin · q = 2"
    private String textoAlgoritmo(){
        int q = configuracion.getQuantumRR();
        switch (configuracion.getAlgoritmoCpu()) {
            case "RR":      return "Round Robin · q = " + q;
            case "SRR":     return "Selfish RR · q = " + q + " · a = " + configuracion.getTasaNuevosSRR()
                                + " · b = " + configuracion.getTasaAceptadosSRR();
            case "Lottery": return "Lottery · q = " + q;
            case "MLFQ":    return "MLFQ · " + configuracion.getNivelesMLFQ() + " niveles · q = " + q
                                + " · promoción " + configuracion.getPeriodoBoostMLFQ() + " s";
            default:        return configuracion.getAlgoritmoCpu();
        }
    }

    // S: String - la estrategia de memoria con su parámetro, p. ej. "Paginación virtual - FIFO · página 8"
    private String textoMemoria(){
        EstrategiaMemoria e = configuracion.getEstrategiaMemoria();
        if (e.esPaginada()) return e + " · página " + configuracion.getTamanoPagina();
        if (e == EstrategiaMemoria.FIJA_IGUAL) return e + " · " + configuracion.getTamanoParticion();
        if (e == EstrategiaMemoria.FIJA_VARIABLE) return e + " · " + configuracion.getListaParticiones();
        return e.toString();
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
