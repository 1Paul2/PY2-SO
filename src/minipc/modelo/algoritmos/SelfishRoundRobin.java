package minipc.modelo.algoritmos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

public class SelfishRoundRobin implements AlgoritmoPlanificacion {
    private final int quantum;
    private final Set<BCP> admitidos = new HashSet<>();
    private long prioridadGrupo;
    private int ultimoReloj;

    public SelfishRoundRobin(int quantum){
        if (quantum < 1) throw new IllegalArgumentException("El quantum debe ser positivo");
        this.quantum = quantum;
    }

    @Override
    public String getNombre(){ return "SRR"; }

    private long prioridadNuevo(BCP proceso, int reloj){
        return 2L * (reloj - proceso.getTiempoLlegada());
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
            prioridadGrupo += reloj - ultimoReloj;
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
