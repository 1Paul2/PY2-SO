package minipc.modelo.algoritmos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

/** Reparte la capacidad entre los procesos residentes, con revisión cada segundo. */
public class GuaranteedScheduling extends PlanificacionPorOrden {
    private final Map<BCP, Double> cuota = new HashMap<>();
    private Set<BCP> participantes = new HashSet<>();
    private int ultimoReloj;

    @Override
    public String getNombre(){ return "Guaranteed"; }

    @Override
    public void actualizarAdmisiones(List<BCP> ocupacion, ColaTrabajo preparados, int reloj){
        // Integrar la cuota del intervalo anterior antes de incorporar nuevas llegadas.
        if (!participantes.isEmpty()) {
            double capacidad = Math.min(1.0, (double) ocupacion.size() / participantes.size());
            double incremento = capacidad * (reloj - ultimoReloj);
            for (BCP proceso : participantes) cuota.merge(proceso, incremento, Double::sum);
        }
        Set<BCP> activos = new HashSet<>(preparados.aLista());
        for (BCP proceso : ocupacion) if (proceso != null) activos.add(proceso);
        cuota.keySet().retainAll(activos);
        for (BCP proceso : activos) cuota.putIfAbsent(proceso, 0.0);
        participantes = activos;
        ultimoReloj = reloj;
    }

    @Override
    protected double puntuacion(BCP proceso){
        double derecho = cuota.getOrDefault(proceso, 0.0);
        if (derecho == 0) return proceso.getTiempoEmpleado() == 0 ? 0 : Double.POSITIVE_INFINITY;
        return proceso.getTiempoEmpleado() / derecho;
    }
}
