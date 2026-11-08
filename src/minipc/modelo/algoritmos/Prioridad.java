package minipc.modelo.algoritmos;

import minipc.modelo.BCP;

/** Prioridad estática expropiativa. Un número menor tiene mayor prioridad. */
public class Prioridad extends PlanificacionPorOrden {
    @Override
    public String getNombre(){ return "Prioridad"; }

    @Override
    protected double puntuacion(BCP proceso){ return proceso.getPrioridad(); }
}
