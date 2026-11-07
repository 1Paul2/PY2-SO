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
 * Correccion - Poll (observación PY1): "se pudo mejorar la escalabilidad del programa para
 * aceptar nuevos algoritmos de planificación". Antes el FCFS estaba escrito directo dentro de
 * Planificador. Ahora el Planificador le pregunta a un AlgoritmoPlanificacion dos cosas:
 *
 *   1. seleccionar(): qué proceso de la cola de preparados entra a la CPU.
 *   2. debeExpropiar(): después de cada segundo, si hay que sacar al proceso de la CPU
 *      (algoritmos expropiativos como RR o SRT). Por defecto no se expropia.
 *
 * Para agregar un algoritmo nuevo basta con crear una clase que implemente esta interfaz
 * y pasársela a Planificador.setAlgoritmo(); no hay que tocar el gestor ni la CPU.
 */
public interface AlgoritmoPlanificacion {

    // E: no aplica
    // S: String - nombre del algoritmo para mostrar en pantalla y en las estadísticas (p. ej. "FCFS")
    // R: ninguna
    String getNombre();

    // E: preparados (ColaTrabajo) - cola de procesos en estado Preparado
    // S: BCP - el proceso elegido, ya sacado de la cola; null si la cola está vacía
    // R: debe sacar de la cola el proceso que devuelve
    BCP seleccionar(ColaTrabajo preparados);

    default BCP seleccionar(ColaTrabajo preparados, int reloj){
        return seleccionar(preparados);
    }

    // E: enEjecucion (BCP) - proceso en la CPU; segundosEnCPU (int) - segundos seguidos que lleva
    //    en la CPU desde que fue despachado; preparados (ColaTrabajo) - procesos esperando la CPU
    // S: boolean - true si el proceso debe salir de la CPU y volver a la cola de preparados
    // R: no debe modificar la cola; los algoritmos no expropiativos devuelven siempre false
    // Índices de CPU a desalojar en una decisión conjunta. Una entrada null representa una CPU libre.
    default java.util.List<Integer> reequilibrar(java.util.List<BCP> ocupacion, ColaTrabajo preparados){
        return java.util.List.of();
    }

    default void actualizarAdmisiones(java.util.List<BCP> ocupacion, ColaTrabajo preparados, int reloj){}

    default boolean debeExpropiar(BCP enEjecucion, int segundosEnCPU, ColaTrabajo preparados){
        return false;
    }
}
