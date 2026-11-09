package minipc.gui;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import minipc.gui.VentanaPrincipal.Componentes.BarraUso;
import minipc.gui.VentanaPrincipal.Componentes.Boton;
import minipc.gui.VentanaPrincipal.Componentes.Distintivo;
import minipc.gui.VentanaPrincipal.Componentes.RenderEstado;
import minipc.gui.VentanaPrincipal.Componentes.Tarjeta;
import minipc.hardware.CPU;
import minipc.hardware.Disco;
import minipc.hardware.Memoria;
import minipc.hardware.EstrategiaMemoria;
import minipc.hardware.GestorMemoria;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;
import minipc.modelo.Instruccion;
import minipc.modelo.Planificador;
import minipc.util.ConfiguracionSistema;
import minipc.util.ParserASM;

/**
 * VentanaPrincipal: interfaz gráfica del Mini PC. Muestra la cola de trabajos,
 * la CPU y sus registros, el BCP actual, la pila, la memoria principal (mapa
 * y tabla), el disco y la pantalla. Toda la lógica la delega en
 * GestorProcesos; esta clase solo dibuja y reacciona a los botones.
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
    private JComboBox<String> cmbCpuDetalle;
    private DefaultTableModel modeloCpus;

    // ---- Cola de trabajos ----
    private Tarjeta tarjetaCola;
    private DefaultTableModel modeloCola;
    private JTable tablaCola;

    // ---- Recursos y dispositivos ----
    private BarraUso barraProcesos, barraMemoria, barraBCP, barraDisco, barraVirtual;
    private Distintivo dispCPU, dispPantalla, dispDisco;

    // ---- Memoria ----
    private Tarjeta tarjetaMemoria;
    private MapaMemoria mapaMemoria;
    private DefaultTableModel modeloMemoria;
    private JTable tablaMemoria;
    private DefaultTableModel modeloPaginas;
    private String[] duenoPosicion = new String[0];   // "S.O.", "P1"  por posición
    private int posicionPC = -1;

    // ---- CPU ----
    private JLabel lblProcesoActual, lblInstruccion;
    private Distintivo distEstadoCPU;
    private final JLabel[] valoresRegistro = new JLabel[10];  // PC, IR, AC, AX, BX, CX, DX, FLAG, AH, AL

    // ---- Pila ----
    private PanelPila panelPila;

    // ---- BCP ----
    private final JLabel[] valoresBCP = new JLabel[12];
    private Distintivo distEstadoBCP;

    // ---- Disco ----
    private Tarjeta tarjetaDisco;
    private DefaultTableModel modeloDisco;
    private JTable tablaDisco;

    // ---- Pantalla ----
    private JTextPane areaPantalla;

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
 

    private JPanel crearCuerpo(){
        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.insets = new Insets(0, 0, 0, 12);

        g.gridx = 0; g.weightx = 0.27;
        JPanel izquierda = crearColumnaIzquierda();
        izquierda.setPreferredSize(new Dimension(380, 700));
        izquierda.setMinimumSize(new Dimension(300, 0));
        cuerpo.add(izquierda, g);
        g.gridx = 1; g.weightx = 0.45;
        JPanel central = crearColumnaCentral();
        central.setPreferredSize(new Dimension(650, 700));
        central.setMinimumSize(new Dimension(400, 0));
        cuerpo.add(central, g);
        g.gridx = 2; g.weightx = 0.28; g.insets = new Insets(0, 0, 0, 0);
        JPanel derecha = crearColumnaDerecha();
        derecha.setPreferredSize(new Dimension(400, 700));
        derecha.setMinimumSize(new Dimension(320, 0));
        cuerpo.add(derecha, g);
        return cuerpo;
    }

    // Columna con altura proporcional por fila
    private JPanel columna(Component[] partes, double[] pesos){
        JPanel col = new JPanel(new GridBagLayout());
        col.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1;
        for (int i = 0; i < partes.length; i++) {
            g.gridy = i;
            g.weighty = pesos[i];
            g.insets = new Insets(0, 0, i < partes.length - 1 ? 12 : 0, 0);
            col.add(partes[i], g);
        }
        return col;
    }

    // ------------------------- Columna izquierda -------------------------

    // Correccion - Poll: se quita el dispositivo "Teclado (entrada)" (INT 09H ya no se usa)
    private JPanel crearColumnaIzquierda(){
        // Cola de trabajos (lista de trabajos + lista de procesos)
        tarjetaCola = new Tarjeta("Cola de trabajos", Tema.VERDE);
        modeloCola = new DefaultTableModel(new String[]{"#", "Programa", "Estado", "Base", "Alcance", "Ráfaga", "Llegada (s)", "CPU"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        tablaCola = new JTable(modeloCola);
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
        tarjetaCola.getContenido().add(Componentes.scroll(tablaCola), BorderLayout.CENTER);
        JScrollPane estados = Componentes.scroll(crearLeyendaEstados());
        estados.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        estados.setPreferredSize(new Dimension(300, 48));
        tarjetaCola.getContenido().add(estados, BorderLayout.SOUTH);

        // Recursos del sistema
        Tarjeta recursos = new Tarjeta("Recursos del sistema", Tema.AZUL);
        JPanel barras = new JPanel(new GridLayout(5, 1, 0, 10));
        barras.setOpaque(false);
        barraProcesos = new BarraUso("Procesos en memoria", Tema.VERDE);
        barraMemoria = new BarraUso("Memoria de usuario", Tema.AZUL);
        barraBCP = new BarraUso("Zona S.O. (BCP)", Tema.NARANJA_SO);
        barraDisco = new BarraUso("Almacenamiento", Tema.MORADO);
        barraVirtual = new BarraUso("Memoria virtual", new Color(0x14B8A6));
        barras.add(barraProcesos);
        barras.add(barraMemoria);
        barras.add(barraVirtual);
        barras.add(barraBCP);
        barras.add(barraDisco);
        recursos.getContenido().add(barras, BorderLayout.NORTH);

        // Dispositivos
        Tarjeta dispositivos = new Tarjeta("Dispositivos", Tema.MORADO);
        JPanel filas = new JPanel(new GridLayout(3, 1, 0, 6));
        filas.setOpaque(false);
        dispCPU = new Distintivo();
        dispPantalla = new Distintivo();
        dispDisco = new Distintivo();
        filas.add(filaDispositivo("CPU ocupadas", dispCPU));
        filas.add(filaDispositivo("Pantalla", dispPantalla));
        filas.add(filaDispositivo("Disco", dispDisco));
        modeloCpus = new DefaultTableModel(new String[]{"CPU", "Proceso", "Estado", "PC"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        JTable tablaCpus = new JTable(modeloCpus);
        Componentes.estilizarTabla(tablaCpus);
        tablaCpus.setRowHeight(22);
        tablaCpus.getSelectionModel().addListSelectionListener(e -> {
            int fila = tablaCpus.getSelectedRow();
            if (!e.getValueIsAdjusting() && fila >= 0 && fila < cmbCpuDetalle.getItemCount())
                cmbCpuDetalle.setSelectedIndex(fila);
        });
        JScrollPane resumenCpus = Componentes.scroll(tablaCpus);
        resumenCpus.setPreferredSize(new Dimension(260, 160));
        resumenCpus.setMinimumSize(new Dimension(220, 160));
        dispositivos.getContenido().add(resumenCpus, BorderLayout.CENTER);
        dispositivos.getContenido().add(filas, BorderLayout.SOUTH);

        return columna(new Component[]{tarjetaCola, recursos, dispositivos}, new double[]{1.0, 0.0, 0.0});
    }

    // Correccion - Poll (observación PY1): la leyenda incluye "Finalizado con error"
    private JPanel crearLeyendaEstados(){
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        p.setOpaque(false);
        for (String e : new String[]{"Nuevo", "Preparado", "Ejecución", "En espera", "Suspendido en espera",
                                     "Suspendido preparado", BCP.FINALIZADO, BCP.FINALIZADO_ERROR}) {
            Distintivo d = new Distintivo();
            d.setEstado(e);
            p.add(d);
        }
        return p;
    }

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

    // -------------------------- Columna central --------------------------

    // Correccion - Poll: se quita la línea de entrada del teclado (etiqueta, campo y botón Enviar)
    // debajo de la pantalla, y la leyenda "(+N virtual)" de la memoria virtual partida
    private JPanel crearColumnaCentral(){
        // Memoria principal: mapa visual + tabla
        tarjetaMemoria = new Tarjeta("Memoria principal", Tema.VERDE);
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

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapaConLeyenda, Componentes.scroll(tablaMemoria));
        split.setResizeWeight(0.32);   // la tabla necesita espacio para leer los campos del BCP
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(8);
        modeloPaginas = new DefaultTableModel(new String[]{"Proceso", "Página", "Base RAM", "Base disco"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        JTable tablaPaginas = new JTable(modeloPaginas);
        Componentes.estilizarTabla(tablaPaginas);
        JTabbedPane vistasMemoria = new JTabbedPane();
        vistasMemoria.addTab("RAM física", split);
        vistasMemoria.addTab("Tabla de páginas", Componentes.scroll(tablaPaginas));
        tarjetaMemoria.getContenido().add(vistasMemoria, BorderLayout.CENTER);

        // Pantalla
        Tarjeta pantalla = new Tarjeta("Pantalla", new Color(0x0F766E));
        areaPantalla = new JTextPane();
        areaPantalla.setEditable(false);
        areaPantalla.setFont(Tema.MONO_NORMAL);
        areaPantalla.setBackground(new Color(0xFBFDFB));
        areaPantalla.setBorder(new EmptyBorder(8, 10, 8, 10));
        pantalla.getContenido().add(Componentes.scroll(areaPantalla), BorderLayout.CENTER);

        // Disco
        tarjetaDisco = new Tarjeta("Disco", Tema.MORADO);
        modeloDisco = new DefaultTableModel(new String[]{"Pos", "Valor en disco", "Zona"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        tablaDisco = new JTable(modeloDisco);
        Componentes.estilizarTabla(tablaDisco);
        tablaDisco.setRowHeight(22);
        RenderDisco renderDisco = new RenderDisco();
        for (int i = 0; i < 3; i++) tablaDisco.getColumnModel().getColumn(i).setCellRenderer(renderDisco);
        tablaDisco.getColumnModel().getColumn(0).setPreferredWidth(64);
        tablaDisco.getColumnModel().getColumn(0).setMinWidth(60);
        tablaDisco.getColumnModel().getColumn(1).setPreferredWidth(220);
        tablaDisco.getColumnModel().getColumn(2).setPreferredWidth(80);
        tarjetaDisco.getContenido().add(Componentes.scroll(tablaDisco), BorderLayout.CENTER);

        JPanel abajo = new JPanel(new GridLayout(1, 2, 12, 0));
        abajo.setOpaque(false);
        abajo.add(pantalla);
        abajo.add(tarjetaDisco);

        return columna(new Component[]{tarjetaMemoria, abajo}, new double[]{0.58, 0.42});
    }

    private JLabel itemLeyenda(String texto, Color c){
        JLabel l = new JLabel(texto, new Componentes.IconoPunto(c, 9), SwingConstants.LEFT);
        l.setFont(Tema.PEQUENA);
        l.setForeground(Tema.TEXTO_SUAVE);
        return l;
    }

    // -------------------------- Columna derecha --------------------------

    // Correccion - Poll: se quita la barra de progreso de la instrucción según su peso
    private JPanel crearColumnaDerecha(){
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
        cmbCpuDetalle.addActionListener(e -> {
            if (gestor != null) {
                actualizarCPU();
                actualizarBCP();
                actualizarMemoria();
            }
        });
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

        JPanel detalle = new JPanel(new BorderLayout());
        detalle.setOpaque(false);
        JScrollPane scroll = Componentes.scroll(columna(new Component[]{cpu, pila, bcp}, new double[]{0.0, 0.0, 1.0}));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        detalle.add(scroll, BorderLayout.CENTER);
        return detalle;
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
        if (cargados > 0) configurarLlegadas();
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
        configurarLlegadas();
        actualizarVista();
    }

    // E: no aplica
    // S: no aplica (void)
    // R: solo se muestran los procesos pendientes; al cancelar se conservan sus llegadas
    private void configurarLlegadas(){
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
            int opcion = JOptionPane.showConfirmDialog(this, scroll,
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
                avisar("Ingrese llegadas entre " + gestor.getReloj() + " y " + maximo + ", y boletos y prioridades enteros positivos.");
            }
        }
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


    // ACTUALIZAR LA VISTA
 

    // E: no aplica
    // S: no aplica (void)
    // R: refresca todos los paneles con el estado actual del GestorProcesos
    private void actualizarVista(){
        actualizarResumenCpus();
        actualizarEncabezado();
        actualizarCola();
        actualizarRecursos();
        actualizarCPU();
        actualizarBCP();
        actualizarMemoria();
        actualizarDisco();
        actualizarPantalla();
        actualizarBotones();
    }

    // Correccion - Poll: se quita la hora real (HH:mm:ss); queda solo el tiempo en segundos desde 0
    private CPU cpuSeleccionada(){
        if (gestor == null) return null;
        int indice = Math.max(0, cmbCpuDetalle.getSelectedIndex());
        return gestor.getCpu(Math.min(indice, gestor.getCantidadCpus() - 1));
    }

    private void actualizarResumenCpus(){
        int cantidad = gestor == null ? configuracion.getCantidadCpus() : gestor.getCantidadCpus();
        if (cmbCpuDetalle.getItemCount() != cantidad) {
            cmbCpuDetalle.removeAllItems();
            for (int i = 1; i <= cantidad; i++) cmbCpuDetalle.addItem("CPU " + i);
            cmbCpuDetalle.setSelectedIndex(0);
        }
        modeloCpus.setRowCount(cantidad);
        for (int i = 0; i < cantidad; i++) {
            CPU cpu = gestor == null ? null : gestor.getCpu(i);
            BCP b = cpu == null ? null : cpu.getBcpActual();
            modeloCpus.setValueAt("CPU " + (i + 1), i, 0);
            modeloCpus.setValueAt(b == null ? "-" : "P" + b.getIdProceso(), i, 1);
            modeloCpus.setValueAt(b == null ? "Ociosa" : "Ejecución", i, 2);
            modeloCpus.setValueAt(b == null ? "-" : cpu.getPC(), i, 3);
        }
    }

    private void actualizarEncabezado(){
        if (gestor == null) {
            ((Distintivo) lblReloj).setColores("Tiempo  0 s", Tema.VERDE_OSCURO, Tema.VERDE_CLARO);
            return;
        }
        ((Distintivo) lblReloj).setColores("Tiempo  " + gestor.getReloj() + " s", Tema.VERDE_OSCURO, Tema.VERDE_CLARO);
    }

    // Correccion - Poll (observación PY1): "Finalizado con error" también oculta la base, igual que "Finalizado"
    private void actualizarCola(){
        modeloCola.setRowCount(0);
        if (gestor == null) {
            tarjetaCola.setExtra("");
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
        tarjetaCola.setExtra(gestor.getTodosLosProcesos().size() + " procesos · " + trabajos + " esperando memoria");
    }

    // Correccion - Poll: se quita el estado del dispositivo teclado, y la memoria usada se cuenta
    // con el alcance completo del proceso (ya no hay parte en memoria virtual)
    // Correccion - Poll (observación PY1): la capacidad de la zona del S.O. se cuenta en bloques de
    // BCP (16 posiciones cada uno), no en posiciones sueltas
    private void actualizarRecursos(){
        if (gestor == null) {
            int usuario = configuracion.getMemoriaPrincipal() - configuracion.getMemoriaSistema();
            barraProcesos.setValor(0, Planificador.MAX_PROCESOS_EN_MEMORIA * configuracion.getCantidadCpus());
            barraMemoria.setValor(0, usuario);
            barraBCP.setValor(0, Math.min(Planificador.MAX_PROCESOS_EN_MEMORIA * configuracion.getCantidadCpus(),
                configuracion.getMemoriaSistema() / BCP.TAMANO_BLOQUE));
            barraDisco.setValor(0, configuracion.getAlmacenamientoSecundario());
            barraVirtual.setValor(0, configuracion.getMemoriaVirtual());
            dispCPU.setColores("Libre", Tema.GRIS, Tema.GRIS_CLARO);
            dispPantalla.setColores("Lista", Tema.VERDE_OSCURO, Tema.VERDE_CLARO);
            dispDisco.setColores("Sin archivos", Tema.GRIS, Tema.GRIS_CLARO);
            return;
        }
        Memoria m = gestor.getMemoria();
        Disco d = gestor.getDisco();
        int usuarioTotal = m.getTamanoTotal() - m.getInicioUsuario();
        int usuarioUsado = 0, bcps = 0;
        for (BCP b : gestor.getTodosLosProcesos()) {
            String e = b.getEstado();
            if (!"Nuevo".equals(e) && !b.estaFinalizado()) {
                // La reserva física puede incluir fragmentación o solo algunas páginas.
                if (b.getDireccionBCP() >= 0) bcps++;
            }
        }
        usuarioUsado = gestor.getGestorMemoria().getReservadoRam();
        int discoUsado = 0, archivos = 0;
        for (int i = 0; i < d.getTamanoTotal(); i++) {
            String v = d.leer(i);
            if (v != null && !v.isEmpty()) {
                discoUsado++;
                if (i < d.getTamanoIndice()) archivos++;
            }
        }
        barraProcesos.setValor(gestor.procesosEnMemoria(), gestor.getPlanificador().getMaxProcesosEnMemoria());
        barraMemoria.setValor(usuarioUsado, usuarioTotal);
        barraBCP.setValor(bcps, gestor.getPlanificador().getCapacidadBCP());
        barraDisco.setValor(discoUsado, d.getTamanoTotal());
        barraVirtual.setValor(d.virtualUsada(), d.getTamanoMemoriaVirtual());

        int ocupadas = gestor.getCpusOcupadas();
        dispCPU.setColores(ocupadas + "/" + gestor.getCantidadCpus() + " ejecutando",
            ocupadas > 0 ? Tema.VERDE_OSCURO : Tema.GRIS,
            ocupadas > 0 ? Tema.VERDE_CLARO : Tema.GRIS_CLARO);

        dispPantalla.setColores(gestor.getPantalla().isEmpty() ? "Lista" : "Mostrando salida",
            Tema.VERDE_OSCURO, Tema.VERDE_CLARO);

        dispDisco.setColores(archivos + " archivo(s)", Tema.MORADO, Tema.MORADO_CLARO);
    }

    // Correccion - Poll: ya no muestra "N de M s" según el peso; cada instrucción dura 1 s.
    // Tampoco muestra "desde memoria virtual" (memoria virtual partida)
    private void actualizarCPU(){
        CPU cpu = gestor == null ? null : cpuSeleccionada();
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
    private void actualizarBCP(){
        BCP b = (gestor == null) ? null : cpuSeleccionada().getBcpActual();
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

    // Correccion - Poll: el dueño de cada posición usa el alcance completo y el PC siempre está
    // en RAM (ya no hay parte del programa en memoria virtual)
    // Correccion - Poll (observación PY1): las 16 posiciones del bloque de cada BCP se marcan como
    // suyas ("BCP P1"); el valor de cada posición ya dice qué campo es ("PC=82")
    private void actualizarMemoria(){
        mapaMemoria.setGestor(gestor);
        int total = gestor == null ? configuracion.getMemoriaPrincipal() : gestor.getMemoria().getTamanoTotal();
        tarjetaMemoria.setExtra(gestor == null ? configuracion.getEstrategiaMemoria().toString()
            : gestor.getGestorMemoria().getEstrategia() + " · fallos " + gestor.getGestorMemoria().getFallosPagina());
        modeloPaginas.setRowCount(0);
        if (gestor == null) {
            modeloMemoria.setRowCount(0);
            duenoPosicion = new String[0];
            posicionPC = -1;
            return;
        }
        Memoria m = gestor.getMemoria();
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
        for (GestorMemoria.Pagina pagina : gestor.getGestorMemoria().getPaginas())
            modeloPaginas.addRow(new Object[]{"P" + pagina.proceso().getIdProceso(), pagina.numero(),
                pagina.baseRam() < 0 ? "-" : pagina.baseRam(), pagina.baseDisco() < 0 ? "-" : pagina.baseDisco()});
        if (modeloMemoria.getRowCount() != total) modeloMemoria.setRowCount(total);
        for (int i = 0; i < total; i++) {
            modeloMemoria.setValueAt(i, i, 0);
            modeloMemoria.setValueAt(m.leer(i), i, 1);
            modeloMemoria.setValueAt(duenoPosicion[i], i, 2);
        }

        BCP actual = cpuSeleccionada().getBcpActual();
        int nuevoPC = actual == null ? -1 : gestor.getGestorMemoria().direccionFisica(
            actual, cpuSeleccionada().getPC() - actual.getDireccionBase());
        valoresRegistro[0].setToolTipText(gestor.getGestorMemoria().getEstrategia().esPaginada()
            ? "PC lógico. La tabla de páginas traduce a RAM o disco." : "Dirección física de la siguiente instrucción");
        if (nuevoPC != posicionPC && nuevoPC >= 0 && nuevoPC < total) {
            tablaMemoria.scrollRectToVisible(tablaMemoria.getCellRect(Math.min(total - 1, nuevoPC + 3), 0, true));
            tablaMemoria.scrollRectToVisible(tablaMemoria.getCellRect(nuevoPC, 0, true));
        }
        posicionPC = nuevoPC;
        tablaMemoria.repaint();
    }

    // Correccion - Poll: la zona virtual del disco ya no marca partes de procesos ni sigue al PC
    // (memoria virtual partida); queda reservada para la paginación
    private void actualizarDisco(){
        if (gestor == null) {
            modeloDisco.setRowCount(0);
            tarjetaDisco.setExtra(configuracion.getAlmacenamientoSecundario() + " posiciones");
            return;
        }
        Disco d = gestor.getDisco();
        int total = d.getTamanoTotal();
        tarjetaDisco.setExtra(total + " · " + d.virtualUsada() + "/" + d.getTamanoMemoriaVirtual() + " virtual");
        if (modeloDisco.getRowCount() != total) modeloDisco.setRowCount(total);
        int inicioVirtual = total - d.getTamanoMemoriaVirtual();
        String[] duenosVirtuales = new String[total];
        for (GestorMemoria.Pagina pagina : gestor.getGestorMemoria().getPaginas()) {
            if (pagina.baseDisco() >= 0) {
                int tamano = configuracion.getTamanoPagina();
                for (int k = 0; k < tamano; k++) duenosVirtuales[pagina.baseDisco() + k] = "P"
                    + pagina.proceso().getIdProceso() + " pág " + pagina.numero();
            }
        }
        for (int i = 0; i < total; i++) {
            String zona = i < d.getTamanoIndice() ? "Índice" : (i >= inicioVirtual ? "Virtual" : "Archivos");
            modeloDisco.setValueAt(i, i, 0);
            modeloDisco.setValueAt(d.leer(i), i, 1);
            modeloDisco.setValueAt(duenosVirtuales[i] == null ? zona : duenosVirtuales[i], i, 2);
        }
        tablaDisco.repaint();
    }

    // Correccion - Poll: se quita la habilitación del campo de teclado según el proceso en espera
    private void actualizarPantalla(){
        StyledDocument doc = areaPantalla.getStyledDocument();
        areaPantalla.setText("");

        if (gestor != null) {
            for (String linea : gestor.getPantalla()) {
                SimpleAttributeSet a = new SimpleAttributeSet();
                StyleConstants.setFontFamily(a, Tema.MONO_NORMAL.getFamily());
                StyleConstants.setFontSize(a, 13);
                if (linea.startsWith("ERROR")) {
                    StyleConstants.setForeground(a, Tema.ROJO);
                    StyleConstants.setBold(a, true);
                } else if (linea.startsWith(">>")) {
                    StyleConstants.setForeground(a, Tema.AMBAR);
                    StyleConstants.setBold(a, true);
                } else if (linea.startsWith("[SO]")) {
                    StyleConstants.setForeground(a, Tema.TEXTO_SUAVE);
                    StyleConstants.setItalic(a, true);
                } else if (linea.startsWith("P")) {
                    StyleConstants.setForeground(a, Tema.VERDE_OSCURO);
                    StyleConstants.setBold(a, true);
                } else {
                    StyleConstants.setForeground(a, Tema.AZUL);
                }
                try {
                    doc.insertString(doc.getLength(), linea + "\n", a);
                } catch (javax.swing.text.BadLocationException ignorada) { }
            }
        }
        areaPantalla.setCaretPosition(doc.getLength());
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

 
    // ESTADÍSTICAS Y CONFIGURACIÓN
 

    // E: no aplica
    // S: no aplica (void)
    // R: muestra la ejecución actual por proceso y el acumulado de todas las ejecuciones
    private void mostrarEstadisticas(){
        boolean hayActual = gestor != null && !gestor.getTodosLosProcesos().isEmpty();
        if (!hayActual && ejecucionesAcumuladas.isEmpty()) {
            avisar("Todavía no hay procesos ni ejecuciones registradas.");
            return;
        }
        JDialog dlg = new JDialog(this, "Estadísticas de ejecución", true);
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
            actual.add(etiqueta("Tiempos en segundos desde 0.  Tr = Final − Llegada.  Ts = ráfaga (líneas a procesar)."),
                BorderLayout.SOUTH);
            pestanas.addTab("Esta ejecución", actual);
        }

        // ---- Acumulado de todas las ejecuciones ----
        DefaultTableModel modeloAcum = new DefaultTableModel(
            new String[]{"#", "Algoritmo", "CPU", "Procesos", "Inicio", "Final", "Ocioso", "Tr promedio", "Tr/Ts promedio"}, 0) {
            @Override public boolean isCellEditable(int f, int c){ return false; }
        };
        for (int i = 0; i < ejecucionesAcumuladas.size(); i++) {
            String[] e = ejecucionesAcumuladas.get(i);
            Object[] fila = new Object[e.length + 1];
            fila[0] = i + 1;
            System.arraycopy(e, 0, fila, 1, e.length);
            modeloAcum.addRow(fila);
        }
        JTable tablaAcum = new JTable(modeloAcum);
        Componentes.estilizarTabla(tablaAcum);
        for (int i = 0; i < modeloAcum.getColumnCount(); i++) tablaAcum.getColumnModel().getColumn(i).setCellRenderer(centro);
        pestanas.addTab("Acumulado (" + ejecucionesAcumuladas.size() + ")", Componentes.scroll(tablaAcum));

        raiz.add(pestanas, BorderLayout.CENTER);

        Boton cerrar = new Boton("Cerrar", Tema.VERDE);
        cerrar.addActionListener(e -> dlg.dispose());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pie.setOpaque(false);
        pie.add(cerrar);
        raiz.add(pie, BorderLayout.SOUTH);

        dlg.setContentPane(raiz);
        dlg.setSize(960, 500);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private JPanel tarjetaDato(String titulo, String valor, Color color){
        Tarjeta t = new Tarjeta(titulo, color);
        JLabel v = new JLabel(valor);
        v.setFont(new Font(Tema.TITULO.getFamily(), Font.BOLD, 24));
        v.setForeground(color);
        t.getContenido().add(v, BorderLayout.CENTER);
        return t;
    }

    // Correccion - Poll: la zona del S.O. y la memoria virtual se pueden editar; ya no se
    // calculan con porcentajes fijos (25 % y 12,5 %)
    private void mostrarConfiguracion(){
        if (gestor != null && gestor.getReloj() > 0 && gestor.hayProcesos()) {
            avisar("La cantidad de CPU se configura antes de ejecutar. Usá Limpiar para iniciar otra simulación.");
            return;
        }
        JDialog dlg = new JDialog(this, "Configuración del Mini PC", true);
        Tarjeta t = new Tarjeta("Tamaños (se guardan en " + ConfiguracionSistema.ARCHIVO_POR_DEFECTO + ")", Tema.VERDE);
        JPanel campos = new JPanel(new GridLayout(9, 2, 10, 10));
        campos.setOpaque(false);
        JSpinner cantidadCpus = new JSpinner(new SpinnerNumberModel(configuracion.getCantidadCpus(), 1, GestorProcesos.MAX_CPUS, 1));
        campos.add(etiqueta("Cantidad de CPU (1 a 5)")); campos.add(cantidadCpus);
        JComboBox<String> algoritmoCpu = new JComboBox<>(new String[]{"FCFS", "SRT", "SPN", "RR", "HRRN", "SRR", "Lottery", "Prioridad", "Guaranteed", "MLFQ"});
        algoritmoCpu.setSelectedItem(configuracion.getAlgoritmoCpu());
        campos.add(etiqueta("Algoritmo de CPU")); campos.add(algoritmoCpu);
        JSpinner quantumRR = new JSpinner(new SpinnerNumberModel(configuracion.getQuantumRR(), 1, Integer.MAX_VALUE, 1));
        quantumRR.setEnabled(("RR".equals(algoritmoCpu.getSelectedItem()) || "SRR".equals(algoritmoCpu.getSelectedItem()) || "Lottery".equals(algoritmoCpu.getSelectedItem()) || "MLFQ".equals(algoritmoCpu.getSelectedItem())));
        algoritmoCpu.addActionListener(e -> quantumRR.setEnabled(("RR".equals(algoritmoCpu.getSelectedItem()) || "SRR".equals(algoritmoCpu.getSelectedItem()) || "Lottery".equals(algoritmoCpu.getSelectedItem()) || "MLFQ".equals(algoritmoCpu.getSelectedItem()))));
        campos.add(etiqueta("Quantum RR / SRR / Lottery / base MLFQ (s)")); campos.add(quantumRR);
        JSpinner nivelesMLFQ = new JSpinner(new SpinnerNumberModel(configuracion.getNivelesMLFQ(), 2, 8, 1));
        JSpinner boostMLFQ = new JSpinner(new SpinnerNumberModel(configuracion.getPeriodoBoostMLFQ(), 1, Integer.MAX_VALUE, 1));
        Runnable habilitarMLFQ = () -> {
            boolean seleccionado = "MLFQ".equals(algoritmoCpu.getSelectedItem());
            nivelesMLFQ.setEnabled(seleccionado);
            boostMLFQ.setEnabled(seleccionado);
        };
        algoritmoCpu.addActionListener(e -> habilitarMLFQ.run());
        habilitarMLFQ.run();
        campos.add(etiqueta("Niveles MLFQ (2 a 8)")); campos.add(nivelesMLFQ);
        campos.add(etiqueta("Promoción global MLFQ (s)")); campos.add(boostMLFQ);
        JTextField mp = campo(configuracion.getMemoriaPrincipal());
        JTextField ms = campo(configuracion.getMemoriaSistema());
        JTextField as = campo(configuracion.getAlmacenamientoSecundario());
        JTextField mv = campo(configuracion.getMemoriaVirtual());
        campos.add(etiqueta("Memoria principal"));               campos.add(mp);
        campos.add(etiqueta("Zona del S.O."));                   campos.add(ms);
        campos.add(etiqueta("Almacenamiento secundario"));       campos.add(as);
        campos.add(etiqueta("Memoria virtual (en disco)"));      campos.add(mv);
        JComboBox<EstrategiaMemoria> estrategiaMemoria = new JComboBox<>(EstrategiaMemoria.values());
        estrategiaMemoria.setSelectedItem(configuracion.getEstrategiaMemoria());
        JTextField tamanoPagina = campo(configuracion.getTamanoPagina());
        JTextField tamanoParticion = campo(configuracion.getTamanoParticion());
        JTextField particiones = new JTextField(configuracion.getListaParticiones());
        JPanel asignacion = new JPanel(new GridLayout(4, 2, 10, 10));
        asignacion.setOpaque(false);
        asignacion.add(etiqueta("Estrategia")); asignacion.add(estrategiaMemoria);
        asignacion.add(etiqueta("Tamaño de página (posiciones)")); asignacion.add(tamanoPagina);
        asignacion.add(etiqueta("Tamaño de partición igual")); asignacion.add(tamanoParticion);
        asignacion.add(etiqueta("Particiones diferentes (ej. 16,32,64)")); asignacion.add(particiones);
        Runnable habilitarMemoria = () -> {
            EstrategiaMemoria estrategia = (EstrategiaMemoria) estrategiaMemoria.getSelectedItem();
            tamanoPagina.setEnabled(estrategia.esPaginada());
            tamanoParticion.setEnabled(estrategia == EstrategiaMemoria.FIJA_IGUAL);
            particiones.setEnabled(estrategia == EstrategiaMemoria.FIJA_VARIABLE);
        };
        estrategiaMemoria.addActionListener(e -> habilitarMemoria.run());
        habilitarMemoria.run();
        JPanel panelAsignacion = new JPanel(new BorderLayout(0, 12));
        panelAsignacion.setOpaque(false);
        panelAsignacion.add(asignacion, BorderLayout.NORTH);
        JLabel nota = etiqueta("<html>Los tamaños usan posiciones de memoria. Cada línea ocupa una posición.<br>"
            + "Paginación virtual usa FIFO y requiere un marco en RAM.<br>"
            + "Los restos de páginas y particiones se reservan como fragmentación interna.<br>"
            + "La zona sobrante que no forma una partición o marco completo queda sin asignar.</html>");
        panelAsignacion.add(nota, BorderLayout.CENTER);
        JTabbedPane pestanasConfiguracion = new JTabbedPane();
        pestanasConfiguracion.addTab("CPU y tamaños", campos);
        pestanasConfiguracion.addTab("Asignación de memoria", panelAsignacion);
        t.getContenido().add(pestanasConfiguracion, BorderLayout.CENTER);
        JLabel capacidad = etiqueta("");
        Runnable mostrarCapacidad = () -> {
            int n = ((Number) cantidadCpus.getValue()).intValue();
            capacidad.setText("Hasta " + (5 * n) + " procesos; " + (5 * n * BCP.TAMANO_BLOQUE)
                + " posiciones de S.O. para sus BCP (además de la RAM de usuario).");
        };
        cantidadCpus.addChangeListener(e -> mostrarCapacidad.run());
        mostrarCapacidad.run();
        t.getContenido().add(capacidad, BorderLayout.SOUTH);

        Boton cargar = new Boton("Cargar archivo...", Tema.GRIS);
        Boton aplicar = new Boton("Guardar y aplicar", Tema.VERDE);
        Boton cancelar = new Boton("Cancelar", new Color(0x94A3B8));

        cargar.addActionListener(e -> {
            JFileChooser sel = new JFileChooser(new File("."));
            if (sel.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                try {
                    configuracion.cargarDesdeArchivo(sel.getSelectedFile());
                    cantidadCpus.setValue(configuracion.getCantidadCpus());
                    algoritmoCpu.setSelectedItem(configuracion.getAlgoritmoCpu());
                    quantumRR.setValue(configuracion.getQuantumRR());
                    nivelesMLFQ.setValue(configuracion.getNivelesMLFQ());
                    boostMLFQ.setValue(configuracion.getPeriodoBoostMLFQ());
                    mp.setText(String.valueOf(configuracion.getMemoriaPrincipal()));
                    ms.setText(String.valueOf(configuracion.getMemoriaSistema()));
                    as.setText(String.valueOf(configuracion.getAlmacenamientoSecundario()));
                    mv.setText(String.valueOf(configuracion.getMemoriaVirtual()));
                    estrategiaMemoria.setSelectedItem(configuracion.getEstrategiaMemoria());
                    tamanoPagina.setText(String.valueOf(configuracion.getTamanoPagina()));
                    tamanoParticion.setText(String.valueOf(configuracion.getTamanoParticion()));
                    particiones.setText(configuracion.getListaParticiones());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        aplicar.addActionListener(e -> {
            try {
                cantidadCpus.commitEdit();
                if (quantumRR.isEnabled()) quantumRR.commitEdit();
                if (nivelesMLFQ.isEnabled()) {
                    nivelesMLFQ.commitEdit();
                    boostMLFQ.commitEdit();
                }
                configuracion.setConfiguracionMemoria(
                    Integer.parseInt(mp.getText().trim()), Integer.parseInt(ms.getText().trim()),
                    Integer.parseInt(as.getText().trim()), Integer.parseInt(mv.getText().trim()),
                    (EstrategiaMemoria) estrategiaMemoria.getSelectedItem(),
                    Integer.parseInt(tamanoPagina.getText().trim()), Integer.parseInt(tamanoParticion.getText().trim()),
                    particiones.getText());
                configuracion.setCantidadCpus(((Number) cantidadCpus.getValue()).intValue());
                configuracion.setAlgoritmoCpu((String) algoritmoCpu.getSelectedItem());
                configuracion.setQuantumRR((Integer) quantumRR.getValue());
                configuracion.setParametrosMLFQ((Integer) nivelesMLFQ.getValue(), (Integer) boostMLFQ.getValue());
                configuracion.guardarEnArchivo(new File(ConfiguracionSistema.ARCHIVO_POR_DEFECTO));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Los cuatro tamaños deben ser números enteros.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean habiaProcesos = gestor != null;
            gestor = null;
            dlg.dispose();
            actualizarVista();
            if (habiaProcesos) {
                JOptionPane.showMessageDialog(this, "Configuración aplicada. Se reinició el sistema: volvé a cargar los programas.");
            }
        });
        cancelar.addActionListener(e -> dlg.dispose());

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pie.setOpaque(false);
        pie.add(cargar);
        pie.add(cancelar);
        pie.add(aplicar);

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(new EmptyBorder(16, 16, 16, 16));
        raiz.add(t, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        dlg.setContentPane(raiz);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private JTextField campo(int valor){
        JTextField f = new JTextField(String.valueOf(valor), 8);
        f.setFont(Tema.MONO_NORMAL);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.BORDE), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }

    private JLabel etiqueta(String t){
        JLabel l = new JLabel(t);
        l.setFont(Tema.NORMAL);
        l.setForeground(Tema.TEXTO);
        return l;
    }

 
    // RENDERIZADORES Y PILA
   

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

    /** Tabla de disco: índice en verde, memoria virtual en morado. */
    private class RenderDisco extends DefaultTableCellRenderer {
        // Correccion - Poll: se quitan el color por proceso en la zona virtual y la marca del PC
        // (memoria virtual partida)
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
            String zona = String.valueOf(t.getModel().getValueAt(r, 2));
            String valor = String.valueOf(t.getModel().getValueAt(r, 1));
            Color fondo = Color.WHITE;
            if ("Índice".equals(zona)) fondo = valor.isEmpty() ? Tema.aclarar(Tema.VERDE, 0.92f) : Tema.VERDE_CLARO;
            else if ("Virtual".equals(zona)) fondo = Tema.aclarar(Tema.MORADO, 0.92f);
            else if (!valor.isEmpty()) fondo = Tema.aclarar(Tema.MORADO, 0.8f);
            l.setBackground(s ? Tema.VERDE_CLARO : fondo);
            l.setForeground(c == 2 ? Tema.TEXTO_SUAVE : Tema.TEXTO);
            l.setFont(c == 1 ? Tema.MONO_NORMAL : Tema.PEQUENA);
            l.setBorder(new EmptyBorder(0, 8, 0, 8));
            return l;
        }
    }

    /** Dibuja las 5 posiciones de la pila del proceso actual con la flecha del tope (SP). */
    private static class PanelPila extends JComponent {
        private BCP bcp;

        PanelPila(){ setPreferredSize(new Dimension(200, 140)); setMinimumSize(new Dimension(200, 120)); }

        void setBCP(BCP b){ this.bcp = b; repaint(); }

        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int filas = 5;
            int gap = 4;
            int alto = Math.max(16, (getHeight() - gap * (filas - 1)) / filas);
            int xCaja = 60, wCaja = getWidth() - xCaja - 10;
            int tope = bcp == null ? -1 : bcp.getTopePila();
            String[] valores = new String[filas];
            if (bcp != null) {
                String p = bcp.pilaTexto();          // "[1,2,3]"
                String interior = p.substring(1, p.length() - 1);
                String[] partes = interior.isEmpty() ? new String[0] : interior.split(",");
                for (int i = 0; i < partes.length && i < filas; i++) valores[i] = partes[i];
            }
            for (int i = filas - 1, fila = 0; i >= 0; i--, fila++) {
                int y = fila * (alto + gap);
                boolean llena = i <= tope;
                g2.setColor(llena ? Tema.AMBAR_CLARO : new Color(0xF8FAF9));
                g2.fillRoundRect(xCaja, y, wCaja, alto, 10, 10);
                g2.setColor(llena ? Tema.AMBAR : Tema.BORDE);
                g2.drawRoundRect(xCaja, y, wCaja - 1, alto - 1, 10, 10);

                g2.setFont(Tema.PEQUENA);
                g2.setColor(Tema.TEXTO_SUAVE);
                g2.drawString(String.valueOf(i), xCaja - 14, y + alto / 2 + 4);

                g2.setFont(Tema.MONO_NORMAL);
                g2.setColor(llena ? Tema.TEXTO : new Color(0xB6C2BB));
                String texto = llena ? valores[i] : "vacío";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(texto, xCaja + (wCaja - fm.stringWidth(texto)) / 2, y + (alto + fm.getAscent()) / 2 - 2);

                if (i == tope) {
                    g2.setFont(Tema.PEQUENA_B);
                    g2.setColor(Tema.AMBAR);
                    g2.drawString("SP", 2, y + alto / 2 + 4);
                    int ax = 22, ay = y + alto / 2;
                    g2.fillPolygon(new int[]{ax + 18, ax + 10, ax + 10}, new int[]{ay, ay - 5, ay + 5}, 3);
                    g2.drawLine(ax, ay, ax + 10, ay);
                }
            }
            if (tope < 0) {
                g2.setFont(Tema.PEQUENA_B);
                g2.setColor(Tema.TEXTO_SUAVE);
                g2.drawString("SP", 2, (filas - 1) * (alto + gap) + alto + 0);
            }
            g2.dispose();
        }
    }


    // CLASES DE APOYO tema, componentes, mapa de memoria
 

    /**
     * Tema: paleta de colores y fuentes de la interfaz. Fondo blanco con acentos
     * verdes; cada estado de proceso y cada proceso en memoria tiene su color.
     */
    public static final class Tema {

        private Tema() {}

        // ---- Base ----
        public static final Color FONDO = new Color(0xF3F6F4);
        public static final Color TARJETA = Color.WHITE;
        public static final Color BORDE = new Color(0xDDE5E0);
        public static final Color TEXTO = new Color(0x1F2A24);
        public static final Color TEXTO_SUAVE = new Color(0x6B7A72);
        public static final Color FILA_ALTERNA = new Color(0xF7FAF8);

        // ---- Acento verde ----
        public static final Color VERDE = new Color(0x16A34A);
        public static final Color VERDE_OSCURO = new Color(0x15803D);
        public static final Color VERDE_CLARO = new Color(0xDCFCE7);
        public static final Color VERDE_MEDIO = new Color(0x86EFAC);

        // ---- Colores de apoyo ----
        public static final Color AZUL = new Color(0x2563EB);
        public static final Color AZUL_CLARO = new Color(0xDBEAFE);
        public static final Color AMBAR = new Color(0xD97706);
        public static final Color AMBAR_CLARO = new Color(0xFEF3C7);
        public static final Color ROJO = new Color(0xDC2626);
        public static final Color ROJO_CLARO = new Color(0xFEE2E2);
        public static final Color MORADO = new Color(0x7C3AED);
        public static final Color MORADO_CLARO = new Color(0xEDE9FE);
        public static final Color GRIS = new Color(0x64748B);
        public static final Color GRIS_CLARO = new Color(0xF1F5F9);
        public static final Color NARANJA_SO = new Color(0xF59E0B);

        // Un color por proceso (se repite si hay más de 8)
        private static final List<Color> COLORES_PROCESO = Arrays.asList(
            new Color(0x22C55E), new Color(0x3B82F6), new Color(0xA855F7), new Color(0x14B8A6),
            new Color(0xEC4899), new Color(0xF97316), new Color(0x6366F1), new Color(0x84CC16)
        );

        // ---- Fuentes ----
        private static final String SANS = elegirFuente("Segoe UI", "Inter", "Helvetica Neue", "SansSerif");
        private static final String MONO = elegirFuente("JetBrains Mono", "Consolas", "Menlo", "Monospaced");

        public static final Font TITULO = new Font(SANS, Font.BOLD, 20);
        public static final Font SUBTITULO = new Font(SANS, Font.PLAIN, 12);
        public static final Font TITULO_CARD = new Font(SANS, Font.BOLD, 14);
        public static final Font NORMAL = new Font(SANS, Font.PLAIN, 13);
        public static final Font NEGRITA = new Font(SANS, Font.BOLD, 13);
        public static final Font PEQUENA = new Font(SANS, Font.PLAIN, 11);
        public static final Font PEQUENA_B = new Font(SANS, Font.BOLD, 11);
        public static final Font MONO_NORMAL = new Font(MONO, Font.PLAIN, 13);
        public static final Font MONO_GRANDE = new Font(MONO, Font.BOLD, 18);

        private static String elegirFuente(String... opciones){
            List<String> disponibles = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String f : opciones) {
                if (disponibles.contains(f)) return f;
            }
            return opciones[opciones.length - 1];
        }

        // E: idProceso (int)
        // S: Color - color fijo que identifica a ese proceso en la memoria y las tablas
        // R: ninguna
        public static Color colorProceso(int idProceso){
            return COLORES_PROCESO.get(Math.floorMod(idProceso - 1, COLORES_PROCESO.size()));
        }

        // E: estado (String) - estado del proceso
        // S: Color[] - {color del texto/borde, color de fondo} para el distintivo del estado
        // R: ninguna
        // Correccion - Poll (observación PY1): "Finalizado con error" se pinta en rojo
        public static Color[] coloresEstado(String estado){
            if (estado == null) return new Color[]{GRIS, GRIS_CLARO};
            switch (estado) {
                case "Nuevo":return new Color[]{GRIS, GRIS_CLARO};
                case "Preparado":return new Color[]{AZUL, AZUL_CLARO};
                case "Ejecución":return new Color[]{VERDE_OSCURO, VERDE_CLARO};
                case "En espera":return new Color[]{AMBAR, AMBAR_CLARO};
                case "Suspendido en espera": return new Color[]{MORADO, MORADO_CLARO};
                case "Suspendido preparado": return new Color[]{new Color(0x9333EA), new Color(0xF3E8FF)};
                case "Finalizado":return new Color[]{new Color(0x475569), new Color(0xE2E8F0)};
                case "Finalizado con error":return new Color[]{ROJO, ROJO_CLARO};
                default:return new Color[]{GRIS, GRIS_CLARO};
            }
        }

        // E: c (Color), factor (float) - 0..1, cuánto aclarar hacia blanco
        // S: Color - versión más clara del color
        // R: ninguna
        public static Color aclarar(Color c, float factor){
            int r = (int) (c.getRed() + (255 - c.getRed()) * factor);
            int g = (int) (c.getGreen() + (255 - c.getGreen()) * factor);
            int b = (int) (c.getBlue() + (255 - c.getBlue()) * factor);
            return new Color(r, g, b);
        }
    }

    /**
     * Componentes: piezas visuales reutilizables de la interfaz (tarjetas con
     * esquinas redondeadas, botones planos, distintivos de estado, barras de uso
     * y estilo de tablas).
     */
    public static final class Componentes {

        private Componentes() {}

        // ======================= Tarjeta =======================

        /** Panel blanco con esquinas redondeadas, franja de color y título. */
        public static class Tarjeta extends JPanel {
            private final JPanel contenido;
            private final JLabel lblExtra;

            // E: titulo (String), acento (Color) - color de la franja y del punto del título
            // S: no aplica (constructor)
            // R: el contenido se agrega con getContenido()
            public Tarjeta(String titulo, Color acento){
                super(new BorderLayout(0, 8));
                setOpaque(false);
                setBorder(new BordeRedondeado(acento));

                JPanel encabezado = new JPanel(new BorderLayout());
                encabezado.setOpaque(false);
                JLabel lbl = new JLabel(titulo);
                lbl.setFont(Tema.TITULO_CARD);
                lbl.setForeground(Tema.TEXTO);
                lbl.setIcon(new IconoPunto(acento, 10));
                lbl.setIconTextGap(8);
                encabezado.add(lbl, BorderLayout.WEST);
                lblExtra = new JLabel();
                lblExtra.setFont(Tema.PEQUENA_B);
                lblExtra.setForeground(Tema.TEXTO_SUAVE);
                encabezado.add(lblExtra, BorderLayout.EAST);
                add(encabezado, BorderLayout.NORTH);

                contenido = new JPanel(new BorderLayout());
                contenido.setOpaque(false);
                add(contenido, BorderLayout.CENTER);
            }

            public JPanel getContenido(){ return contenido; }

            // Texto pequeño a la derecha del título (p. ej. "3 / 5")
            public void setExtra(String texto){ lblExtra.setText(texto); }

            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.TARJETA);
                g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 16, 16));
                g2.dispose();
                super.paintComponent(g);
            }
        }

        /** Borde redondeado con una franja de color arriba. */
        public static class BordeRedondeado extends AbstractBorder {
            private final Color acento;
            public BordeRedondeado(Color acento){ this.acento = acento; }

            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int w, int h){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Shape forma = new RoundRectangle2D.Float(x + 1, y + 1, w - 3, h - 3, 16, 16);
                g2.setClip(forma);
                g2.setColor(acento);
                g2.fillRect(x, y, w, 4);
                g2.setClip(null);
                g2.setColor(Tema.BORDE);
                g2.draw(forma);
                g2.dispose();
            }

            @Override
            public Insets getBorderInsets(Component c){ return new Insets(14, 14, 12, 14); }
        }

        /** Ícono circular de color (para títulos y leyendas). */
        public static class IconoPunto implements Icon {
            private final Color color;
            private final int tam;
            public IconoPunto(Color color, int tam){ this.color = color; this.tam = tam; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(x, y, tam, tam);
                g2.dispose();
            }
            @Override public int getIconWidth(){ return tam; }
            @Override public int getIconHeight(){ return tam; }
        }

        // ======================= Botón =======================

        /** Botón plano redondeado con color propio y efecto al pasar el mouse. */
        public static class Boton extends JButton {
            private Color base;
            private boolean encima = false;

            public Boton(String texto, Color base){
                super(texto);
                this.base = base;
                setFont(Tema.NEGRITA);
                setForeground(Color.WHITE);
                setContentAreaFilled(false);
                setFocusPainted(false);
                setBorderPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setBorder(new EmptyBorder(9, 16, 9, 16));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e){ encima = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e){ encima = false; repaint(); }
                });
            }

            public void setColorBase(Color c){ this.base = c; repaint(); }

            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = !isEnabled() ? new Color(0xCBD5E1) : (encima ? base.darker() : base);
                g2.setColor(c);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        }

        // ======================= Distintivo (badge) =======================

        /** Etiqueta con forma de píldora y colores según el estado. */
        public static class Distintivo extends JLabel {
            private Color fondo = Tema.GRIS_CLARO;

            public Distintivo(){
                setFont(Tema.PEQUENA_B);
                setHorizontalAlignment(CENTER);
                setBorder(new EmptyBorder(3, 10, 3, 10));
            }

            // E: estado (String) - texto a mostrar; se colorea con Tema.coloresEstado
            // S: no aplica (void)
            // R: ninguna
            public void setEstado(String estado){
                Color[] c = Tema.coloresEstado(estado);
                setText(estado == null ? "-" : estado);
                setForeground(c[0]);
                fondo = c[1];
                repaint();
            }

            public void setColores(String texto, Color frente, Color fondo){
                setText(texto);
                setForeground(frente);
                this.fondo = fondo;
                repaint();
            }

            @Override
            protected void paintComponent(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                FontMetrics fm = g2.getFontMetrics(getFont());
                int w = Math.min(getWidth() - 2, fm.stringWidth(getText()) + 20);
                int h = Math.min(getHeight() - 2, fm.getHeight() + 6);
                int x;
                if (getHorizontalAlignment() == LEFT) x = 0;
                else if (getHorizontalAlignment() == RIGHT) x = getWidth() - w - 1;
                else x = (getWidth() - w) / 2;
                int y = (getHeight() - h) / 2;
                g2.setColor(fondo);
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        }

        /** Renderizador de celdas que muestra el estado como distintivo de color. */
        public static class RenderEstado extends DefaultTableCellRenderer {
            private final Distintivo d = new Distintivo();
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int fila, int col){
                d.setEstado(v == null ? "-" : v.toString());
                d.setOpaque(true);
                d.setBackground(sel ? Tema.VERDE_CLARO : (fila % 2 == 0 ? Color.WHITE : Tema.FILA_ALTERNA));
                return d;
            }
        }

        // ======================= Barra de uso =======================

        /** Barra de progreso redondeada con etiqueta y valor. */
        public static class BarraUso extends JPanel {
            private final JLabel lblValor;
            private final Color color;
            private double fraccion = 0;

            public BarraUso(String nombre, Color color){
                super(new BorderLayout(0, 4));
                this.color = color;
                setOpaque(false);
                JPanel arriba = new JPanel(new BorderLayout());
                arriba.setOpaque(false);
                JLabel lbl = new JLabel(nombre);
                lbl.setFont(Tema.NORMAL);
                lbl.setForeground(Tema.TEXTO);
                lbl.setIcon(new IconoPunto(color, 8));
                lbl.setIconTextGap(6);
                lblValor = new JLabel("-");
                lblValor.setFont(Tema.PEQUENA_B);
                lblValor.setForeground(Tema.TEXTO_SUAVE);
                arriba.add(lbl, BorderLayout.WEST);
                arriba.add(lblValor, BorderLayout.EAST);
                add(arriba, BorderLayout.NORTH);

                JComponent barra = new JComponent() {
                    @Override protected void paintComponent(Graphics g){
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int h = getHeight();
                        g2.setColor(Tema.aclarar(color, 0.85f));
                        g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                        int w = (int) Math.round(getWidth() * Math.max(0, Math.min(1, fraccion)));
                        if (w > 0) {
                            g2.setColor(color);
                            g2.fillRoundRect(0, 0, Math.max(w, h), h, h, h);
                        }
                        g2.dispose();
                    }
                };
                barra.setPreferredSize(new Dimension(10, 8));
                add(barra, BorderLayout.CENTER);
            }

            // E: usado, total (int)
            // S: no aplica (void)
            // R: total > 0
            public void setValor(int usado, int total){
                fraccion = total <= 0 ? 0 : (double) usado / total;
                lblValor.setText(usado + " / " + total);
                repaint();
            }
        }

        // ======================= Tablas =======================

        // E: tabla (JTable)
        // S: no aplica (void)
        // R: aplica el estilo del tema: filas altas, sin rejilla vertical, encabezado verde claro
        public static void estilizarTabla(JTable tabla){
            tabla.setFont(Tema.NORMAL);
            tabla.setRowHeight(26);
            tabla.setShowVerticalLines(false);
            tabla.setGridColor(new Color(0xEEF2EF));
            tabla.setSelectionBackground(Tema.VERDE_CLARO);
            tabla.setSelectionForeground(Tema.TEXTO);
            tabla.setFillsViewportHeight(true);
            tabla.setIntercellSpacing(new Dimension(0, 1));
            tabla.setDefaultEditor(Object.class, null);
            JTableHeader h = tabla.getTableHeader();
            h.setReorderingAllowed(false);
            h.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
                    JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                    l.setFont(Tema.PEQUENA_B);
                    l.setForeground(Tema.VERDE_OSCURO);
                    l.setBackground(Tema.VERDE_CLARO);
                    l.setBorder(new EmptyBorder(6, 8, 6, 8));
                    return l;
                }
            });
        }

        // E: tabla (JTable)
        // S: JScrollPane - la tabla dentro de un scroll sin borde
        // R: ninguna
        public static JScrollPane scroll(JComponent c){
            JScrollPane sp = new JScrollPane(c);
            sp.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
            sp.getViewport().setBackground(Color.WHITE);
            sp.getVerticalScrollBar().setUnitIncrement(16);
            sp.setPreferredSize(new Dimension(200, 120));   // deja que el layout decida el alto
            return sp;
        }
    }

    /**
     * MapaMemoria: dibuja la memoria principal como una barra vertical dividida
     * en zonas: S.O. (con los BCP), un bloque de color por proceso cargado y el
     * espacio libre. Marca con una flecha la posición del PC.
     */
    public static class MapaMemoria extends JComponent {

        private GestorProcesos gestor;

        public MapaMemoria(){
            setPreferredSize(new Dimension(260, 300));
            setToolTipText("");
        }

        public void setGestor(GestorProcesos gestor){
            this.gestor = gestor;
            repaint();
        }

        // Correccion - Poll (observación PY1): "Finalizado con error" tampoco tiene memoria asignada
        // Procesos que tienen memoria asignada en este momento
        private List<BCP> procesosEnMemoria(){
            List<BCP> lista = new ArrayList<>();
            if (gestor == null) return lista;
            for (BCP b : gestor.getTodosLosProcesos()) {
                String e = b.getEstado();
                if (!"Nuevo".equals(e) && !b.estaFinalizado() && b.getDireccionBase() >= 0) {
                    lista.add(b);
                }
            }
            return lista;
        }

        // Correccion - Poll: cada bloque se dibuja con el alcance completo; se quitan la etiqueta
        // "(+N virtual)" y el aviso "PC en memoria virtual" (memoria virtual partida)
        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int margenIzq = 44, margenDer = 18, arriba = 4, abajo = 4;
            int x = margenIzq;
            int w = getWidth() - margenIzq - margenDer;
            int h = getHeight() - arriba - abajo;

            if (gestor == null) {
                g2.setColor(Tema.GRIS_CLARO);
                g2.fillRoundRect(x, arriba, w, h, 12, 12);
                g2.setColor(Tema.TEXTO_SUAVE);
                g2.setFont(Tema.NORMAL);
                dibujarCentrado(g2, "Cargá programas para ver la memoria", x, arriba, w, h);
                g2.dispose();
                return;
            }

            int total = gestor.getMemoria().getTamanoTotal();
            int inicioUsuario = gestor.getMemoria().getInicioUsuario();
            double escala = (double) h / total;

            // Fondo: espacio libre con rayas suaves
            g2.setColor(new Color(0xF8FAF9));
            g2.fillRoundRect(x, arriba, w, h, 12, 12);
            g2.setClip(x, arriba, w, h);
            g2.setColor(new Color(0xEAF0EC));
            for (int i = -h; i < w + h; i += 10) {
                g2.drawLine(x + i, arriba + h, x + i + h, arriba);
            }
            g2.setClip(null);

            // Zona del S.O.
            int ySO = arriba;
            int hSO = Math.max(14, (int) Math.round(inicioUsuario * escala));
            g2.setColor(Tema.aclarar(Tema.NARANJA_SO, 0.55f));
            g2.fillRoundRect(x, ySO, w, hSO, 10, 10);
            g2.setColor(Tema.NARANJA_SO.darker());
            g2.setFont(Tema.PEQUENA_B);
            int bcps = 0;
            for (BCP b : procesosEnMemoria()) if (b.getDireccionBCP() >= 0) bcps++;
            dibujarCentrado(g2, "S.O. · " + bcps + " BCP", x, ySO, w, hSO);
            etiquetaDireccion(g2, 0, ySO);

            for (GestorMemoria.Bloque bloque : gestor.getGestorMemoria().getBloquesFisicos()) {
                int y = arriba + (int) Math.round(bloque.base() * escala);
                int alto = Math.max(1, (int) Math.round(bloque.tamanio() * escala));
                BCP proceso = bloque.proceso();
                g2.setColor(Tema.colorProceso(proceso.getIdProceso()));
                g2.fillRect(x, y, w, alto);
                g2.setColor(Color.WHITE);
                g2.setFont(Tema.PEQUENA_B);
                dibujarCentrado(g2, "P" + proceso.getIdProceso() + " · " + bloque.base()
                    + "–" + (bloque.base() + bloque.tamanio() - 1), x, y, w, alto);
            }
            for (CPU cpu : gestor.getCpus()) {
                BCP proceso = cpu.getBcpActual();
                if (proceso == null) continue;
                int pc = gestor.getGestorMemoria().direccionFisica(proceso, cpu.getPC() - proceso.getDireccionBase());
                if (pc < 0) continue;
                int yPC = arriba + (int) Math.round((pc + 0.5) * escala);
                int xf = x + w + 2;
                g2.setColor(Tema.ROJO);
                g2.fillPolygon(new int[]{xf, xf + 10, xf + 10}, new int[]{yPC, yPC - 4, yPC + 4}, 3);
            }

            etiquetaDireccion(g2, total - 1, arriba + h - 10);
            g2.dispose();
        }

        private void etiquetaDireccion(Graphics2D g2, int dir, int y){
            g2.setFont(Tema.PEQUENA);
            g2.setColor(Tema.TEXTO_SUAVE);
            String s = String.format("%04d", dir);
            g2.drawString(s, 4, y + 11);
        }

        private void dibujarCentrado(Graphics2D g2, String s, int x, int y, int w, int h){
            FontMetrics fm = g2.getFontMetrics();
            if (h < fm.getHeight() - 4) return;
            int tx = x + (w - fm.stringWidth(s)) / 2;
            int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(s, tx, ty);
        }

        @Override
        public String getToolTipText(java.awt.event.MouseEvent e){
            if (gestor == null) return null;
            int total = gestor.getMemoria().getTamanoTotal();
            int h = getHeight() - 8;
            int pos = (int) ((e.getY() - 4) / ((double) h / total));
            if (pos < 0 || pos >= total) return null;
            return "Posición " + pos + ": " + gestor.getMemoria().leer(pos);
        }
    }
}
