package minipc.modelo.algoritmos;

import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

public class RoundRobin implements AlgoritmoPlanificacion {
    private final int quantum;

    public RoundRobin(int quantum){
        if (quantum < 1) throw new IllegalArgumentException("El quantum debe ser positivo");
        this.quantum = quantum;
    }

    @Override
    public String getNombre(){
        return "RR";
    }

    @Override
    public BCP seleccionar(ColaTrabajo preparados){
        return preparados.desencolar();
    }

    @Override
    public boolean debeExpropiar(BCP actual, int segundosEnCPU, ColaTrabajo preparados){
        // Si nadie espera, el proceso comienza otro turno en la misma CPU.
        return segundosEnCPU > 0 && segundosEnCPU % quantum == 0 && !preparados.estaVacia();
    }
}
