package minipc.modelo.algoritmos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

/**
 * Selfish Round Robin: los procesos aceptados se turnan la CPU con Round Robin. Los nuevos
 * esperan afuera; su prioridad sube a ritmo "a" y la de los aceptados a ritmo "b". Un nuevo
 * entra a la rotación cuando su prioridad alcanza la de los aceptados.
 * Con b = 0 se comporta como RR; con b >= a, como FCFS.
 */
public class SelfishRoundRobin implements AlgoritmoPlanificacion {
    public static final int TASA_NUEVOS_DEF = 2;
    public static final int TASA_ACEPTADOS_DEF = 1;

    private final int quantum;
    private final int tasaNuevos;      // a: cuánto sube por segundo la prioridad de un proceso nuevo
    private final int tasaAceptados;   // b: cuánto sube por segundo la prioridad de los aceptados
    private final Set<BCP> admitidos = new HashSet<>();
    private long prioridadGrupo;
    private int ultimoReloj;

    public SelfishRoundRobin(int quantum){
        this(quantum, TASA_NUEVOS_DEF, TASA_ACEPTADOS_DEF);
    }

    // E: quantum (int) - segundos por turno; tasaNuevos (int) - a; tasaAceptados (int) - b
    // S: no aplica (constructor)
    // R: quantum >= 1, a >= 1 y b >= 0
    public SelfishRoundRobin(int quantum, int tasaNuevos, int tasaAceptados){
        if (quantum < 1) throw new IllegalArgumentException("El quantum debe ser positivo");
        validarTasas(tasaNuevos, tasaAceptados);
        this.quantum = quantum;
        this.tasaNuevos = tasaNuevos;
        this.tasaAceptados = tasaAceptados;
    }

    // E: tasaNuevos (int) - a; tasaAceptados (int) - b
    // S: no aplica (void)
    // R: lanza IllegalArgumentException si a < 1 o b < 0
    public static void validarTasas(int tasaNuevos, int tasaAceptados){
        if (tasaNuevos < 1 || tasaAceptados < 0)
            throw new IllegalArgumentException("Selfish RR requiere tasa de nuevos (a) >= 1 y tasa de aceptados (b) >= 0");
    }

    @Override
    public String getNombre(){ return "SRR"; }

    private long prioridadNuevo(BCP proceso, int reloj){
        return (long) tasaNuevos * (reloj - proceso.getTiempoLlegada());
    }

    @Override
    public void actualizarAdmisiones(List<BCP> ocupacion, ColaTrabajo preparados, int reloj){
        List<BCP> cola = preparados.aLista();
        Set<BCP> activos = new HashSet<>(cola);
        activos.addAll(ocupacion);
        admitidos.retainAll(activos);
        if (admitidos.isEmpty()) {
            prioridadGrupo = cola.stream().mapToLong(p -> prioridadNuevo(p, reloj)).max().orElse(0);
        } else {
            prioridadGrupo += (long) tasaAceptados * (reloj - ultimoReloj);
        }
        ultimoReloj = reloj;

        // Al alcanzar al grupo, el proceso se incorpora al final de su rotacion.
        for (BCP proceso : cola) {
            if (prioridadNuevo(proceso, reloj) >= prioridadGrupo) admitir(proceso, preparados);
        }
        // En varias CPU, incorporar nuevos si el grupo no alcanza para ocuparlas.
        for (BCP proceso : cola) {
            if (admitidos.size() >= ocupacion.size()) break;
            admitir(proceso, preparados);
        }
    }

    private void admitir(BCP proceso, ColaTrabajo preparados){
        if (admitidos.add(proceso)) {
            preparados.quitar(proceso);
            preparados.encolar(proceso);
        }
    }

    @Override
    public BCP seleccionar(ColaTrabajo preparados){
        for (BCP proceso = preparados.getPrimero(); proceso != null; proceso = proceso.getSiguiente()) {
            if (admitidos.contains(proceso)) {
                preparados.quitar(proceso);
                return proceso;
            }
        }
        return null;
    }

    @Override
    public boolean debeExpropiar(BCP actual, int segundosEnCPU, ColaTrabajo preparados){
        if (segundosEnCPU <= 0 || segundosEnCPU % quantum != 0) return false;
        for (BCP proceso = preparados.getPrimero(); proceso != null; proceso = proceso.getSiguiente()) {
            if (admitidos.contains(proceso)) return true;
        }
        return false;
    }
}
