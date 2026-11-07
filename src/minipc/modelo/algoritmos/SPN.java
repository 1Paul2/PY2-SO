package minipc.modelo.algoritmos;

import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

public class SPN implements AlgoritmoPlanificacion {
    @Override
    public String getNombre(){
        return "SPN";
    }

    @Override
    public BCP seleccionar(ColaTrabajo preparados){
        BCP mejor = preparados.getPrimero();
        for (BCP candidato = mejor; candidato != null; candidato = candidato.getSiguiente()) {
            if (candidato.getRafaga() < mejor.getRafaga()) {
                mejor = candidato;
            }
        }
        if (mejor != null) preparados.quitar(mejor);
        return mejor;
    }
}
