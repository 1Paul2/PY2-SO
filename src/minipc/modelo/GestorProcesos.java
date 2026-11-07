package minipc.modelo;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import minipc.hardware.CPU;
import minipc.hardware.Memoria;
import minipc.hardware.Disco;
import minipc.util.ConfiguracionSistema;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * GestorProcesos: punto de entrada del "sistema operativo". Conecta CPU,
 * memoria, disco, planificador y despachador. Cada llamada a ejecutarUnPaso()
 * equivale a 1 segundo global para todas las CPU (botón "Paso a paso" / "Siguiente").
 */
public class GestorProcesos {

    public static final int MAX_CPUS = 5;
    private final List<CPU> cpus = new ArrayList<>();
    private final List<Despachador> despachadores = new ArrayList<>();
    private final List<String> pantalla = new ArrayList<>();
    private Memoria memoria;
    private Disco disco;
    private Planificador planificador;
    private int siguienteIdProceso;

    private List<BCP> todosLosProcesos;   // todos los procesos creados, en orden de llegada
    private final List<BCP> pendientesLlegada = new ArrayList<>();
    private int reloj;                    // segundos simulados transcurridos
    private final int[] segundosEnCPU;    // quantum independiente por CPU
    private LocalTime horaArranque;       // hora real en que arrancó el sistema

    public static final int MAX_SEGUNDOS_AUTOMATICO = 100000;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    // E: tamaños de memoria principal, zona del S.O., disco y memoria virtual
    // S: no aplica (constructor)
    // R: tamanoSistema < tamanoMemoria; tamanoDisco > 10 + tamanoVirtual
    public GestorProcesos(int tamanoMemoria, int tamanoSistema, int tamanoDisco, int tamanoVirtual) {
        this(tamanoMemoria, tamanoSistema, tamanoDisco, tamanoVirtual, 1);
    }

    public GestorProcesos(int tamanoMemoria, int tamanoSistema, int tamanoDisco,
                         int tamanoVirtual, int cantidadCpus) {
        if (cantidadCpus < 1 || cantidadCpus > MAX_CPUS)
            throw new IllegalArgumentException("La cantidad de CPU debe estar entre 1 y 5");
        this.memoria = new Memoria(tamanoMemoria, tamanoSistema);
        this.disco = new Disco(tamanoDisco, tamanoVirtual);
        this.planificador = new Planificador(memoria, disco, cantidadCpus);
        this.segundosEnCPU = new int[cantidadCpus];
        for (int i = 0; i < cantidadCpus; i++) {
            CPU cpu = new CPU(memoria, pantalla);
            cpu.setDisco(disco);
            cpus.add(cpu);
            despachadores.add(new Despachador(cpu));
        }
        this.siguienteIdProceso = 1;
        this.todosLosProcesos = new ArrayList<>();
        this.reloj = 0;
        this.horaArranque = LocalTime.now();
    }

    public GestorProcesos(ConfiguracionSistema c) {
        this(c.getMemoriaPrincipal(), c.getMemoriaSistema(), c.getAlmacenamientoSecundario(), c.getMemoriaVirtual(), c.getCantidadCpus());
        if ("SRT".equals(c.getAlgoritmoCpu())) {
            planificador.setAlgoritmo(new minipc.modelo.algoritmos.SRT());
        } else if ("SPN".equals(c.getAlgoritmoCpu())) {
            planificador.setAlgoritmo(new minipc.modelo.algoritmos.SPN());
        } else if ("RR".equals(c.getAlgoritmoCpu())) {
            planificador.setAlgoritmo(new minipc.modelo.algoritmos.RoundRobin(c.getQuantumRR()));
        } else if ("HRRN".equals(c.getAlgoritmoCpu())) {
            planificador.setAlgoritmo(new minipc.modelo.algoritmos.HRRN());
        } else if ("SRR".equals(c.getAlgoritmoCpu())) {
            planificador.setAlgoritmo(new minipc.modelo.algoritmos.SelfishRoundRobin(c.getQuantumRR()));
        }
    }

    // ===================== Carga de programas =====================

    // Correccion - Poll: el límite ya no suma la memoria virtual; el programa debe caber en la
    // memoria de usuario (la memoria virtual se rehace con paginación)
    // E: instrucciones (List<Instruccion>) - programa ya validado; nombreArchivo (String)
    // S: BCP - el proceso creado
    // R: lanza RuntimeException si el programa no cabe en memoria o no hay espacio en disco
    public BCP cargarPrograma(List<Instruccion> instrucciones, String nombreArchivo){
        return cargarPrograma(instrucciones, nombreArchivo, reloj);
    }

    // E: instrucciones (List<Instruccion>) - programa ya validado; nombreArchivo (String);
    //    llegada (int) - segundo en que el proceso llega al sistema
    // S: BCP - el proceso creado, en estado Nuevo y sin memoria hasta que llegue
    // R: llegada >= reloj. La ráfaga son las líneas a procesar: no cuenta comentarios ni líneas
    //    vacías, y como cada instrucción dura 1 s también es el tiempo de CPU que necesita
    public BCP cargarPrograma(List<Instruccion> instrucciones, String nombreArchivo, int llegada){
        if (llegada < reloj) throw new IllegalArgumentException("La llegada no puede ser anterior al reloj actual (" + reloj + ")");
        int espacioUsuario = memoria.getTamanoTotal() - memoria.getInicioUsuario();
        if (instrucciones.size() > espacioUsuario) {
            // Desbordamiento de memoria: el programa es más grande que toda la memoria de usuario
            throw new RuntimeException("Desbordamiento de memoria: " + nombreArchivo + " ocupa "
                + instrucciones.size() + " posiciones y la memoria de usuario tiene " + espacioUsuario);
        }

        List<String> lineas = new ArrayList<>();
        for (Instruccion instr : instrucciones) {
            lineas.add(instr.getTextoCompleto());
        }

        // Guardar el programa en disco (si ya existe con el mismo contenido, se reutiliza)
        if (disco.existeArchivo(nombreArchivo)) {
            if (!disco.leerArchivo(nombreArchivo).equals(lineas)) {
                throw new RuntimeException("Ya existe en disco un archivo llamado " + nombreArchivo
                    + " con otro contenido");
            }
        } else if (!disco.guardarArchivo(nombreArchivo, lineas)) {
            throw new RuntimeException("No hay espacio en disco para " + nombreArchivo
                + " (el índice admite " + disco.getTamanoIndice() + " archivos y el área de archivos tiene "
                + (disco.getInicioVirtual() - disco.getTamanoIndice()) + " posiciones)");
        }

        BCP bcp = new BCP(siguienteIdProceso, "Nuevo");
        bcp.setTamanioProceso(instrucciones.size());
        bcp.setRafaga(instrucciones.size());
        bcp.setTiempoLlegada(llegada);
        bcp.agregarArchivoAbierto(nombreArchivo);
        bcp.setPrioridad(siguienteIdProceso);       // FCFS: prioridad = orden de llegada
        siguienteIdProceso++;

        todosLosProcesos.add(bcp);
        pendientesLlegada.add(bcp);
        return bcp;
    }

    // E: bcp (BCP) - proceso que todavía no llega; llegada (int) - nuevo segundo de llegada
    // S: no aplica (void)
    // R: el proceso debe seguir pendiente y llegada >= reloj
    public void configurarLlegada(BCP bcp, int llegada){
        if (!pendientesLlegada.contains(bcp)) throw new IllegalStateException("El proceso ya llegó");
        if (llegada < reloj) throw new IllegalArgumentException("La llegada debe ser al menos " + reloj);
        bcp.setTiempoLlegada(llegada);
    }

    // Pasa a la lista de trabajos los procesos cuya llegada ya se cumplió
    private void admitirLlegadas(){
        // Orden estable: empates por orden de carga, FCFS por llegada real.
        pendientesLlegada.sort(java.util.Comparator.comparingInt(BCP::getTiempoLlegada));
        java.util.Iterator<BCP> it = pendientesLlegada.iterator();
        while (it.hasNext()) {
            BCP b = it.next();
            if (b.getTiempoLlegada() > reloj) break;
            planificador.admitirBCP(b);
            it.remove();
        }
        planificador.prepararSiguientes(enEjecucion());
        mostrarMensajesSO();
    }

    // ===================== Ejecución =====================

    // E: no aplica
    // S: boolean - true si se ejecutó un segundo de CPU; false si no había nada que ejecutar
    // R: 1 llamada = 1 segundo global; hasta una instrucción por CPU
    // Correccion - Poll: se quita el bloqueo por teclado (INT 09H); la CPU solo queda
    // ociosa si no hay procesos preparados
    // Correccion - Poll (observación PY1): un error deja el proceso "Finalizado con error", y después
    // de cada segundo se le pregunta al algoritmo de planificación si debe expropiar al proceso
    // (FCFS nunca lo hace; RR, SRT y los demás lo usarán)
    public boolean ejecutarUnPaso(){
        admitirLlegadas();
        despacharLibres();
        if (enEjecucion() == 0 && pendientesLlegada.isEmpty()) return false;

        // Un paso es un segundo GLOBAL: cada CPU ejecuta como máximo una instrucción.
        // No despachar reemplazos hasta que todas hayan completado este intervalo.
        reloj++;
        boolean[] errores = new boolean[cpus.size()];
        for (int i = 0; i < cpus.size(); i++) {
            CPU cpu = cpus.get(i);
            BCP actual = cpu.getBcpActual();
            if (actual == null) continue;
            segundosEnCPU[i]++;
            actual.setTiempoEmpleado(actual.getTiempoEmpleado() + 1);
            try {
                cpu.tick();
            } catch (RuntimeException e) {
                imprimir("ERROR CPU " + (i + 1) + " P" + actual.getIdProceso() + ": " + e.getMessage());
                errores[i] = true;
            }
        }
        // Liberar primero todos los terminados; así las llegadas ven la RAM disponible.
        for (int i = 0; i < cpus.size(); i++) {
            if (cpus.get(i).getBcpActual() != null && (errores[i] || cpus.get(i).programaTerminado()))
                finalizar(i, errores[i]);
        }
        admitirLlegadas();
        actualizarAdmisionesCpu();
        // Las nuevas llegadas son visibles antes de evaluar expropiación/quantum.
        for (int i = 0; i < cpus.size(); i++) {
            BCP actual = cpus.get(i).getBcpActual();
            if (actual != null && planificador.debeExpropiar(actual, segundosEnCPU[i])) expropiar(i);
        }
        despacharLibres();
        actualizarBCPsEnMemoria();
        return true;
    }

    // Correccion - Poll: ya no se detiene esperando el teclado (INT 09H se quitó)
    // E: no aplica
    // S: no aplica (void)
    // R: se detiene cuando no queda nada que ejecutar, o tras MAX_SEGUNDOS_AUTOMATICO
    public void ejecutarTodo(){
        int segundos = 0;
        while (hayProcesos() && segundos < MAX_SEGUNDOS_AUTOMATICO) {
            if (!ejecutarUnPaso()) break;
            segundos++;
        }
        if (segundos >= MAX_SEGUNDOS_AUTOMATICO) {
            imprimir("Ejecución detenida tras " + MAX_SEGUNDOS_AUTOMATICO + " segundos (¿ciclo infinito?)");
        }
    }

    private void finalizar(int indice, boolean conError){
        CPU cpu = cpus.get(indice);
        BCP bcp = cpu.getBcpActual();
        despachadores.get(indice).capturar(bcp);
        bcp.setTiempoFin(reloj);
        planificador.terminar(bcp, conError);
        cpu.setBcpActual(null);
    }

    private void expropiar(int indice){
        CPU cpu = cpus.get(indice);
        BCP bcp = cpu.getBcpActual();
        despachadores.get(indice).capturar(bcp);
        planificador.devolverAPreparados(bcp);
        cpu.setBcpActual(null);
        imprimir("[SO] Cambio de contexto: " + planificador.getAlgoritmo().getNombre()
            + " expropia a P" + bcp.getIdProceso() + " en CPU " + (indice + 1)
            + " tras " + segundosEnCPU[indice] + " s");
    }

    private void actualizarAdmisionesCpu(){
        List<BCP> ocupacion = new ArrayList<>();
        for (CPU cpu : cpus) ocupacion.add(cpu.getBcpActual());
        planificador.getAlgoritmo().actualizarAdmisiones(ocupacion, planificador.getListaProcesos(), reloj);
    }

    private void despacharLibres(){
        actualizarAdmisionesCpu();
        List<BCP> ocupacion = new ArrayList<>();
        for (CPU cpu : cpus) ocupacion.add(cpu.getBcpActual());
        for (int indice : planificador.getAlgoritmo().reequilibrar(ocupacion, planificador.getListaProcesos())) {
            expropiar(indice);
        }
        planificador.prepararSiguientes(enEjecucion());
        mostrarMensajesSO();
        for (int i = 0; i < cpus.size(); i++) {
            if (cpus.get(i).getBcpActual() != null) continue;
            BCP siguiente = planificador.siguienteProceso(reloj);
            if (siguiente == null) break;
            segundosEnCPU[i] = 0;
            if (siguiente.getTiempoInicio() < 0) siguiente.setTiempoInicio(reloj);
            siguiente.setCpuId(i + 1);
            despachadores.get(i).despachar(siguiente);
            imprimir("[SO] Despachador: P" + siguiente.getIdProceso() + " entra a CPU " + (i + 1));
        }
    }

    private void mostrarMensajesSO(){
        for (String m : planificador.tomarMensajes()) imprimir(m);
    }

    private int enEjecucion(){
        int ocupadas = 0;
        for (CPU cpu : cpus) if (cpu.getBcpActual() != null) ocupadas++;
        return ocupadas;
    }

    // Correccion - Poll (observación PY1): cada BCP ocupa un bloque de BCP.TAMANO_BLOQUE posiciones
    // Escribe cada BCP activo en su bloque de la zona del S.O., un campo por posición
    private void actualizarBCPsEnMemoria(){
        for (int i = 0; i < cpus.size(); i++) {
            BCP enCpu = cpus.get(i).getBcpActual();
            if (enCpu != null) {
                despachadores.get(i).capturar(enCpu);
                escribirBCP(enCpu);
            }
        }
        for (BCP b = planificador.getListaProcesos().getPrimero(); b != null; b = b.getSiguiente()) {
            escribirBCP(b);
        }
        for (BCP b = planificador.getListaEspera().getPrimero(); b != null; b = b.getSiguiente()) {
            escribirBCP(b);
        }
    }

    // Correccion - Poll (observación PY1): antes el BCP entero se escribía como un solo texto en una
    // posición; ahora cada campo (estado, PC, IR, AC, …, Sig) va en su propia posición del bloque
    private void escribirBCP(BCP b){
        if (b.getDireccionBCP() < 0) return;
        String[] celdas = b.aCeldasMemoria();
        for (int i = 0; i < celdas.length; i++) {
            memoria.escribir(b.getDireccionBCP() + i, celdas[i]);
        }
    }

    // ===================== Consultas para la GUI =====================

    public boolean hayProcesos(){
        return !pendientesLlegada.isEmpty() || enEjecucion() > 0
            || !planificador.getListaTrabajos().estaVacia()
            || !planificador.getListaProcesos().estaVacia()
            || !planificador.getListaEspera().estaVacia();
    }

    // E: no aplica
    // S: List<BCP> - copia de los procesos que todavía no llegan
    // R: ninguna
    public List<BCP> getPendientesLlegada(){
        return new ArrayList<>(pendientesLlegada);
    }

    public List<BCP> getTodosLosProcesos(){
        return todosLosProcesos;
    }

    public int procesosEnMemoria(){
        return planificador.procesosEnMemoria(enEjecucion());
    }

    public int getReloj(){ return reloj; }

    // Correccion - Poll: se quitó getHoraSimulada(); horaDe() queda solo porque la usan las
    // estadísticas, que por ahora se mantienen como en el proyecto 1
    // E: segundos (int) - segundos simulados desde el arranque
    // S: String - hora "HH:mm:ss" correspondiente
    // R: ninguna
    public String horaDe(int segundos){
        return horaArranque.plusSeconds(segundos).format(FORMATO_HORA);
    }

    // E: no aplica
    // S: List<String[]> - por proceso: id, archivo, inicio, fin, duración (s), tiempo CPU (s), estado
    // R: los procesos sin terminar muestran "-" en fin y duración
    public List<String[]> getEstadisticas(){
        List<String[]> filas = new ArrayList<>();
        for (BCP b : todosLosProcesos) {
            boolean inicio = b.getTiempoInicio() >= 0;
            boolean fin = b.getTiempoFin() >= 0;
            filas.add(new String[]{
                "P" + b.getIdProceso(),
                b.getArchivosAbiertos().isEmpty() ? "-" : b.getArchivosAbiertos().get(0),
                inicio ? horaDe(b.getTiempoInicio()) : "-",
                fin ? horaDe(b.getTiempoFin()) : "-",
                (inicio && fin) ? String.valueOf(b.getTiempoFin() - b.getTiempoInicio()) : "-",
                String.valueOf(b.getTiempoEmpleado()),
                b.getEstado()
            });
        }
        return filas;
    }

    // Compatibilidad con clientes de una sola CPU; los índices de getCpu(int) parten de 0.
    public CPU getCpu() { return cpus.get(0); }
    public CPU getCpu(int indice) { return cpus.get(indice); }
    public List<CPU> getCpus() { return java.util.Collections.unmodifiableList(cpus); }
    public int getCantidadCpus() { return cpus.size(); }
    public int getCpusOcupadas() { return enEjecucion(); }
    public void imprimir(String texto) { pantalla.add(texto); }
    public List<String> getPantalla() { return java.util.Collections.unmodifiableList(pantalla); }
    public Memoria getMemoria() { return memoria; }
    public Disco getDisco() { return disco; }
    public Planificador getPlanificador() { return planificador; }
    public Despachador getDespachador() { return despachadores.get(0); }
}
