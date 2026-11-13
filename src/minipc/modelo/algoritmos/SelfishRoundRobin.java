package minipc.modelo.algoritmos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

/**
 * Selfish Round Robin (Stallings, Operating Systems: Internals and Design Principles, 9.ª ed.,
 * problema 9.12 y figura 9.19): los procesos nuevos esperan y su prioridad sube desde 0 a la
 * tasa "a"; cuando alcanza la prioridad de los aceptados, entran a compartir la CPU con Round
 * Robin, y desde ahí su prioridad sube a una tasa menor "b" (0 <= b < a).
 * Con b = 0 los nuevos entran enseguida y se comporta como RR.
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
    // R: quantum >= 1 y 0 <= b < a
    public SelfishRoundRobin(int quantum, int tasaNuevos, int tasaAceptados){
        if (quantum < 1) throw new IllegalArgumentException("El quantum debe ser positivo");
        validarTasas(tasaNuevos, tasaAceptados);
        this.quantum = quantum;
        this.tasaNuevos = tasaNuevos;
        this.tasaAceptados = tasaAceptados;
    }

    // E: tasaNuevos (int) - a; tasaAceptados (int) - b
    // S: no aplica (void)
    // R: lanza IllegalArgumentException si no se cumple 0 <= b < a, como pide el libro
    public static void validarTasas(int tasaNuevos, int tasaAceptados){
        if (tasaAceptados < 0 || tasaAceptados >= tasaNuevos)
            throw new IllegalArgumentException("Selfish RR requiere 0 <= b < a: la tasa de los aceptados (b = "
                + tasaAceptados + ") debe ser menor que la de los nuevos (a = " + tasaNuevos + ")");
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
