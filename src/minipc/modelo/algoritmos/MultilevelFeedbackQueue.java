package minipc.modelo.algoritmos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

/** RR por nivel, descenso por consumo acumulado y promoción global periódica. */
public class MultilevelFeedbackQueue extends PlanificacionPorOrden {
    private static class Estado {
        int nivel;
        long consumo;
        int ultimoTiempoCpu;
        boolean turnoVencido;
    }

    private final int niveles;
    private final int quantumBase;
    private final int periodoBoost;
    private final Map<BCP, Estado> estados = new HashMap<>();
    private int ultimoBoost;

    public MultilevelFeedbackQueue(int niveles, int quantumBase, int periodoBoost){
        if (niveles < 2 || niveles > 8 || quantumBase < 1 || periodoBoost < 1)
            throw new IllegalArgumentException("MLFQ requiere 2 a 8 niveles, quantum y promoción positivos");
        this.niveles = niveles;
        this.quantumBase = quantumBase;
        this.periodoBoost = periodoBoost;
    }

    @Override
    public String getNombre(){ return "MLFQ"; }

    @Override
    public void actualizarAdmisiones(List<BCP> ocupacion, ColaTrabajo preparados, int reloj){
        Set<BCP> activos = new HashSet<>(preparados.aLista());
        for (BCP proceso : ocupacion) if (proceso != null) activos.add(proceso);
        estados.keySet().retainAll(activos);
        for (BCP proceso : activos) {
            Estado estado = estados.computeIfAbsent(proceso, p -> new Estado());
            estado.consumo += proceso.getTiempoEmpleado() - estado.ultimoTiempoCpu;
            estado.ultimoTiempoCpu = proceso.getTiempoEmpleado();
            long quantum = (long) quantumBase << estado.nivel;
            if (estado.consumo >= quantum) {
                estado.nivel = Math.min(niveles - 1, estado.nivel + 1);
                estado.consumo = 0;
                estado.turnoVencido = true;
            }
        }
        int periodo = reloj / periodoBoost;
        if (periodo > ultimoBoost) {
            for (Estado estado : estados.values()) {
                estado.nivel = 0;
                estado.consumo = 0;
                estado.turnoVencido = true;
            }
            ultimoBoost = periodo;
        }
    }

    @Override
    protected double puntuacion(BCP proceso){
        return estados.computeIfAbsent(proceso, p -> new Estado()).nivel;
    }

    @Override
    public BCP seleccionar(ColaTrabajo preparados){
        BCP elegido = super.seleccionar(preparados);
        if (elegido != null) estados.get(elegido).turnoVencido = false;
        return elegido;
    }

    @Override
    public boolean debeExpropiar(BCP actual, int segundosEnCPU, ColaTrabajo preparados){
        return estados.get(actual).turnoVencido;
    }
}
