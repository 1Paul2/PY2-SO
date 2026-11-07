package minipc.modelo.algoritmos;

import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

public class HRRN implements AlgoritmoPlanificacion {
    @Override
    public String getNombre(){
        return "HRRN";
    }

    @Override
    public BCP seleccionar(ColaTrabajo preparados){
        return seleccionar(preparados, 0);
    }

    @Override
    public BCP seleccionar(ColaTrabajo preparados, int reloj){
        BCP mejor = null;
        double mayorRelacion = Double.NEGATIVE_INFINITY;
        for (BCP candidato = preparados.getPrimero(); candidato != null; candidato = candidato.getSiguiente()) {
            int espera = Math.max(0, reloj - candidato.getTiempoLlegada());
            int servicio = Math.max(1, candidato.getRafaga());
            double relacion = 1.0 + (double) espera / servicio;
            if (relacion > mayorRelacion) {
                mejor = candidato;
                mayorRelacion = relacion;
            }
        }
        if (mejor != null) preparados.quitar(mejor);
        return mejor;
    }
}
