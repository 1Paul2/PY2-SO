package minipc.modelo;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import minipc.hardware.Memoria;
import minipc.hardware.Disco;
import minipc.modelo.algoritmos.AlgoritmoPlanificacion;
import minipc.modelo.algoritmos.FCFS;
import java.util.ArrayList;
import java.util.List;

/**
 * Planificador: administra las colas de procesos y la memoria que ocupan.
 *   - Planificador de trabajos (largo plazo): prepararSiguientes() pasa programas de la lista
 *     de trabajos (en disco) a memoria, hasta MAX_PROCESOS_EN_MEMORIA.
 *   - Planificador de CPU (corto plazo): siguienteProceso() y debeExpropiar() delegan en el
 *     AlgoritmoPlanificacion elegido (FCFS por defecto).
 */
public class Planificador {

    // Colas
    private ColaTrabajo listaTrabajos;     // Nuevo: en disco, sin memoria
    private ColaTrabajo listaProcesos;     // Preparado: en memoria, esperando CPU
    private ColaTrabajo listaEspera;       // En espera: bloqueado por E/S 
    private ColaTrabajo listaFinalizados;  // Finalizado

    private Memoria memoria;
    private final minipc.hardware.GestorMemoria gestorMemoria;
    private Disco disco;

    // Bloques de la zona del S.O. donde se guardan los BCP: el bloque i empieza en la
    // posición i * BCP.TAMANO_BLOQUE (true = ocupado)
    private boolean[] slotsBCP;

    // Algoritmo del planificador de CPU (FCFS por defecto)
    private AlgoritmoPlanificacion algoritmo = new FCFS();

    public static final int MAX_PROCESOS_EN_MEMORIA = 5; // por CPU
    private final int maxProcesosEnMemoria;

    // Avisos del S.O. (p. ej. uso de memoria virtual) que el gestor muestra en la consola
    private final List<String> mensajes = new ArrayList<>();

    // E: no aplica
    // S: List<String> - avisos pendientes; la lista interna queda vacía
    // R: ninguna
    public List<String> tomarMensajes(){
        List<String> copia = new ArrayList<>(mensajes);
        mensajes.clear();
        return copia;
    }

    // Correccion - Poll (observación PY1): cada BCP ocupa un bloque de BCP.TAMANO_BLOQUE posiciones
    // (antes 1 sola), así que la cantidad de BCP que caben es zona del S.O. / TAMANO_BLOQUE
    // E: memoria (Memoria), disco (Disco)
    // S: no aplica (constructor)
    // R: la zona del S.O. debe tener al menos BCP.TAMANO_BLOQUE posiciones para guardar un BCP
    public Planificador(Memoria memoria, Disco disco) {
        this(memoria, disco, 1);
    }

    public Planificador(Memoria memoria, Disco disco, int cantidadCpus) {
        this(memoria, disco, cantidadCpus, new minipc.hardware.GestorMemoria(memoria, disco, null));
    }

    public Planificador(Memoria memoria, Disco disco, int cantidadCpus, minipc.hardware.GestorMemoria gestorMemoria) {
        this.gestorMemoria = gestorMemoria;
        if (cantidadCpus < 1 || cantidadCpus > GestorProcesos.MAX_CPUS)
            throw new IllegalArgumentException("La cantidad de CPU debe estar entre 1 y 5");
        this.maxProcesosEnMemoria = MAX_PROCESOS_EN_MEMORIA * cantidadCpus;
        this.listaTrabajos = new ColaTrabajo();
        this.listaProcesos = new ColaTrabajo();
        this.listaEspera = new ColaTrabajo();
        this.listaFinalizados = new ColaTrabajo();
        this.memoria = memoria;
        this.disco = disco;
        int slots = Math.min(maxProcesosEnMemoria, memoria.getInicioUsuario() / BCP.TAMANO_BLOQUE);
        this.slotsBCP = new boolean[slots];
    }

    // E: no aplica
    // S: int - cuántos BCP caben en la zona del S.O. (como máximo MAX_PROCESOS_EN_MEMORIA)
    // R: ninguna
    public int getMaxProcesosEnMemoria(){ return maxProcesosEnMemoria; }

    public int getCapacidadBCP(){
        return slotsBCP.length;
    }

    // Correccion - Poll (observación PY1): el algoritmo de CPU se puede cambiar sin tocar el resto
    public AlgoritmoPlanificacion getAlgoritmo(){
        return algoritmo;
    }

    // E: algoritmo (AlgoritmoPlanificacion) - algoritmo del planificador de CPU
    // S: no aplica (void)
    // R: no puede ser null
    public void setAlgoritmo(AlgoritmoPlanificacion algoritmo){
        if (algoritmo == null) throw new IllegalArgumentException("El algoritmo de planificación no puede ser null");
        this.algoritmo = algoritmo;
    }

    // E: bcp (BCP) - proceso recién creado
    // S: no aplica (void)
    // R: ninguna
    public void admitirBCP(BCP bcp){
        bcp.setEstado("Nuevo");
        listaTrabajos.encolar(bcp);
    }

    // E: enEjecucion (int) - cantidad de CPU ocupadas, 0 si no
    // S: no aplica (void)
    // R: respeta FCFS estricto: si el primero no cabe, los de atrás esperan.
    //    Si el primero no puede entrar (límite de 5 procesos o falta de memoria) y hay
    //    un proceso en memoria bloqueado esperando el teclado, ese proceso se SUSPENDE
    //    (sale de la memoria) para darle espacio.
    public void prepararSiguientes(int enEjecucion) {
        while (!listaTrabajos.estaVacia()) {
            boolean cargado = procesosEnMemoria(enEjecucion) < maxProcesosEnMemoria
                && intentarCargar(listaTrabajos.getPrimero());
            if (cargado) continue;
            if (!suspenderUnoEnEspera(listaTrabajos.getPrimero())) break;   // nadie para suspender: espera
        }
    }

    // Si no puede reservarse el programa completo, permanece en la lista de trabajos.
    // E: bcp (BCP) - primer trabajo de la lista de trabajos
    // S: boolean - true si se pudo cargar en memoria (y pasó a la lista de procesos)
    // R: el gestor de memoria reserva según la estrategia seleccionada
    // Correccion - Poll (observación PY1): el BCP recibe la dirección donde empieza su bloque
    // (bloque * BCP.TAMANO_BLOQUE), no un índice de una sola posición
    private boolean intentarCargar(BCP bcp){
        int slot = slotLibre();
        if (slot == -1) return false;                        // no hay espacio para su BCP

        List<String> lineas = disco.leerArchivo(bcp.getArchivosAbiertos().get(0));
        if (!gestorMemoria.cargar(bcp, lineas)) return false;
        int hueco = bcp.getDireccionBase();

        if (bcp.getDesplazamientoPC() >= 0) {
            // Proceso que estaba suspendido: se reubica y continúa donde iba
            bcp.setPcGuardado(hueco + bcp.getDesplazamientoPC());
            bcp.setDesplazamientoPC(-1);
            mensajes.add("[SO] P" + bcp.getIdProceso() + " vuelve a memoria (nueva base " + hueco + ")");
        } else {
            bcp.setPcGuardado(hueco);
        }

        slotsBCP[slot] = true;
        bcp.setDireccionBCP(slot * BCP.TAMANO_BLOQUE);
        bcp.setEstado("Preparado");

        listaTrabajos.desencolar();
        listaProcesos.encolar(bcp);
        return true;
    }

    // E: necesita (BCP) - el trabajo que no pudo entrar a memoria
    // S: boolean - true si se suspendió algún proceso
    // R: solo se suspenden procesos "En espera" (bloqueados por teclado), que no usan la CPU.
    //    Su programa sigue guardado en disco, así que al volver se recarga desde ahí.
    private boolean suspenderUnoEnEspera(BCP necesita){
        for (BCP b = listaEspera.getPrimero(); b != null; b = b.getSiguiente()) {
            if (!"En espera".equals(b.getEstado())) continue;

            b.setDesplazamientoPC(b.getPcGuardado() - b.getDireccionBase());
            liberarMemoria(b);
            b.setEstado(BCP.SUSPENDIDO_ESPERA);      
            mensajes.add("[SO] P" + b.getIdProceso() + " suspendido: sale de memoria mientras espera el teclado,"
                + " para dar espacio a P" + necesita.getIdProceso());
            return true;
        }
        return false;
    }

    // Libera la reserva completa, incluidos marcos y respaldo virtual.
    // Correccion - Poll (observación PY1): libera el bloque completo del BCP (TAMANO_BLOQUE posiciones)
    // Libera la RAM y el bloque del BCP de un proceso
    private void liberarMemoria(BCP bcp){
        gestorMemoria.liberar(bcp);
        if (bcp.getDireccionBCP() >= 0) {
            memoria.liberar(bcp.getDireccionBCP(), BCP.TAMANO_BLOQUE);
            slotsBCP[bcp.getDireccionBCP() / BCP.TAMANO_BLOQUE] = false;
            bcp.setDireccionBCP(-1);
        }
        bcp.setDireccionBase(-1);
    }

    // E: enEjecucion (int) - cantidad de CPU ocupadas
    // S: int - procesos que ocupan memoria (los suspendidos no cuentan)
    // R: ninguna
    public int procesosEnMemoria(int enEjecucion){
        int enEspera = 0;
        for (BCP b = listaEspera.getPrimero(); b != null; b = b.getSiguiente()) {
            if (!b.getEstado().startsWith("Suspendido")) enEspera++;
        }
        return listaProcesos.tamanio() + enEspera + enEjecucion;
    }

    private int slotLibre(){
        for (int i = 0; i < slotsBCP.length; i++) {
            if (!slotsBCP[i]) return i;
        }
        return -1;
    }

    // Correccion - Poll (observación PY1): ya no está escrito FCFS aquí; el algoritmo elegido
    // decide qué proceso de la cola de preparados entra a la CPU
    // E: no aplica
    // S: BCP - el proceso que eligió el algoritmo (ya fuera de la cola), o null si no hay
    // R: ninguna
    public BCP siguienteProceso(){
        return siguienteProceso(0);
    }

    public BCP siguienteProceso(int reloj){
        return algoritmo.seleccionar(listaProcesos, reloj);
    }

    // Correccion - Poll (observación PY1): punto de expropiación para algoritmos como RR o SRT
    // E: enEjecucion (BCP) - proceso en la CPU; segundosEnCPU (int) - segundos seguidos en la CPU
    // S: boolean - true si el algoritmo pide sacarlo de la CPU
    // R: ninguna
    public boolean debeExpropiar(BCP enEjecucion, int segundosEnCPU){
        return algoritmo.debeExpropiar(enEjecucion, segundosEnCPU, listaProcesos);
    }

    // Correccion - Poll (observación PY1): un proceso expropiado vuelve al final de la cola de
    // preparados sin perder su memoria ni su BCP
    // E: bcp (BCP) - proceso que salió de la CPU sin terminar
    // S: no aplica (void)
    // R: ninguna
    public void devolverAPreparados(BCP bcp){
        bcp.setEstado("Preparado");
        listaProcesos.encolar(bcp);
    }

    // E: bcp (BCP) - proceso que pidió entrada de teclado
    // S: no aplica (void)
    // R: ninguna
    public void enEspera(BCP bcp){
        bcp.setEstado("En espera");
        listaEspera.encolar(bcp);
    }

    // E: no aplica
    // S: BCP - el primer proceso en espera, ya movido al final de la cola de preparados
    // R: devuelve null si no hay procesos en espera
    public BCP salirDeEspera(){
        BCP bcp = listaEspera.desencolar();
        if (bcp == null) return null;
        if (bcp.getEstado().startsWith("Suspendido")) {
            // Ya ocurrió su evento, pero sigue fuera de memoria
            bcp.setEstado(BCP.SUSPENDIDO_PREPARADO);
            // No está en memoria: vuelve a la lista de trabajos para recargarse cuando haya espacio
            listaTrabajos.encolar(bcp);
        } else {
            bcp.setEstado("Preparado");
            listaProcesos.encolar(bcp);
        }
        return bcp;
    }

    // Correccion - Poll (observación PY1): recibe si el proceso terminó por un error, para dejarlo
    // como "Finalizado con error" en vez de "Finalizado"
    // E: bcp (BCP) - proceso que terminó; conError (boolean) - true si terminó por un error
    // S: no aplica (void)
    // R: libera su memoria y el bloque de su BCP
    public void terminar(BCP bcp, boolean conError) {
        bcp.setEstado(conError ? BCP.FINALIZADO_ERROR : BCP.FINALIZADO);
        int base = bcp.getDireccionBase();
        liberarMemoria(bcp);
        bcp.setDireccionBase(base);       // se conserva para las estadísticas
        listaFinalizados.encolar(bcp);
    }

    public ColaTrabajo getListaTrabajos() {
        return listaTrabajos; 
    }
    public ColaTrabajo getListaProcesos() {
        return listaProcesos; 
    }
    public ColaTrabajo getListaEspera() {
        return listaEspera; 
    }
    public ColaTrabajo getListaFinalizados() {
        return listaFinalizados; 
    }
}
