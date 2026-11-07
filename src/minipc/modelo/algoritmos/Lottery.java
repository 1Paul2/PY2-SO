package minipc.modelo.algoritmos;

import java.util.Objects;
import java.util.Random;
import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

public class Lottery implements AlgoritmoPlanificacion {
    private final int quantum;
    private final Random azar;

    public Lottery(int quantum){
        this(quantum, new Random());
    }

    public Lottery(int quantum, Random azar){
        if (quantum < 1) throw new IllegalArgumentException("El quantum debe ser positivo");
        this.quantum = quantum;
        this.azar = Objects.requireNonNull(azar);
    }

    @Override
    public String getNombre(){ return "Lottery"; }

    @Override
    public BCP seleccionar(ColaTrabajo preparados){
        long total = 0;
        for (BCP proceso = preparados.getPrimero(); proceso != null; proceso = proceso.getSiguiente()) {
            total += proceso.getBoletos();
        }
        if (total == 0) return null;
        long ganador = azar.nextLong(total);
        for (BCP proceso = preparados.getPrimero(); proceso != null; proceso = proceso.getSiguiente()) {
            ganador -= proceso.getBoletos();
            if (ganador < 0) {
                preparados.quitar(proceso);
                return proceso;
            }
        }
        throw new IllegalStateException("La cola cambio durante el sorteo");
    }

    @Override
    public boolean debeExpropiar(BCP actual, int segundosEnCPU, ColaTrabajo preparados){
        return segundosEnCPU > 0 && segundosEnCPU % quantum == 0 && !preparados.estaVacia();
    }
}
