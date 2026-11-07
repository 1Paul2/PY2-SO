package minipc.modelo.algoritmos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

/** Menor puntuación recibe preferencia. En empates conserva la CPU actual. */
abstract class PlanificacionPorOrden implements AlgoritmoPlanificacion {
    protected abstract double puntuacion(BCP proceso);

    @Override
    public BCP seleccionar(ColaTrabajo preparados){
        BCP mejor = null;
        for (BCP candidato = preparados.getPrimero(); candidato != null; candidato = candidato.getSiguiente()) {
            if (mejor == null || puntuacion(candidato) < puntuacion(mejor)) mejor = candidato;
        }
        if (mejor != null) preparados.quitar(mejor);
        return mejor;
    }

    @Override
    public List<Integer> reequilibrar(List<BCP> ocupacion, ColaTrabajo preparados){
        List<BCP> candidatos = preparados.aLista();
        candidatos.sort(Comparator.comparingDouble(this::puntuacion));
        List<Integer> ocupadas = new ArrayList<>();
        for (int i = 0; i < ocupacion.size(); i++) {
            if (ocupacion.get(i) != null) ocupadas.add(i);
        }
        ocupadas.sort(Comparator.comparingDouble((Integer i) -> puntuacion(ocupacion.get(i))).reversed());
        int candidato = ocupacion.size() - ocupadas.size();
        List<Integer> desalojos = new ArrayList<>();
        for (int indice : ocupadas) {
            if (candidato >= candidatos.size()
                || puntuacion(candidatos.get(candidato)) >= puntuacion(ocupacion.get(indice))) break;
            desalojos.add(indice);
            candidato++;
        }
        return desalojos;
    }
}
