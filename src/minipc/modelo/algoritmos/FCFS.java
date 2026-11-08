package minipc.modelo.algoritmos;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import minipc.modelo.BCP;
import minipc.modelo.ColaTrabajo;

/**
 * Correccion - Poll (observación PY1): FCFS (First Come, First Served) como primera
 * implementación de AlgoritmoPlanificacion. Antes vivía dentro de Planificador.siguienteProceso().
 * Atiende a los procesos en el orden en que llegaron a la cola de preparados y nunca expropia:
 * el proceso sale de la CPU solo cuando termina.
 */
public class FCFS implements AlgoritmoPlanificacion {

    @Override
    public String getNombre(){
        return "FCFS";
    }

    // E: preparados (ColaTrabajo) - cola de preparados
    // S: BCP - el primero de la cola (el que llegó antes), o null si está vacía
    // R: ninguna
    @Override
    public BCP seleccionar(ColaTrabajo preparados){
        return preparados.desencolar();
    }
}
