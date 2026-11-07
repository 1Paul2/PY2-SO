package minipc.modelo.algoritmos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

public class SRT implements AlgoritmoPlanificacion {
    private static int restante(BCP proceso){
        // La ráfaga es una estimación estática: un salto puede prolongar el programa.
        return Math.max(1, proceso.getRafaga() - proceso.getTiempoEmpleado());
    }

    @Override public String getNombre(){ return "SRT"; }

    @Override public BCP seleccionar(ColaTrabajo preparados){
        BCP mejor = null;
        for (BCP candidato : preparados.aLista()) {
            if (mejor == null || restante(candidato) < restante(mejor)) mejor = candidato;
        }
        if (mejor != null) preparados.quitar(mejor);
        return mejor;
    }

    @Override
    public List<Integer> reequilibrar(List<BCP> ocupacion, ColaTrabajo preparados){
        List<BCP> candidatos = preparados.aLista();
        candidatos.sort(Comparator.comparingInt(SRT::restante));
        List<Integer> ocupadas = new ArrayList<>();
        for (int i = 0; i < ocupacion.size(); i++) {
            if (ocupacion.get(i) != null) ocupadas.add(i);
        }
        ocupadas.sort(Comparator.comparingInt((Integer i) -> restante(ocupacion.get(i))).reversed());

        // Los primeros candidatos usan las CPU libres. Cada candidato restante
        // puede desplazar a una sola CPU, empezando por la de mayor tiempo restante.
        int candidato = ocupacion.size() - ocupadas.size();
        List<Integer> desalojos = new ArrayList<>();
        for (int indice : ocupadas) {
            if (candidato >= candidatos.size()
                || restante(candidatos.get(candidato)) >= restante(ocupacion.get(indice))) break;
            desalojos.add(indice);
            candidato++;
        }
        return desalojos;
    }
}
