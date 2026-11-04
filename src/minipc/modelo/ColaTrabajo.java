/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package minipc.modelo;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

/**
 * ColaTrabajo: lista enlazada de BCP. Cada BCP apunta al siguiente con getSiguiente(), así que
 * la cola es el mismo enlace "siguiente BCP" que se guarda en la memoria del S.O. Se usa para la
 * lista de trabajos, la de preparados, la de espera y la de finalizados.
 */
public class ColaTrabajo {
    //Atributos de ColaTrabajo
    private BCP primero;
    private BCP ultimo;
    
    //Constructor de ColaTrabajo
    public ColaTrabajo(){
        this.primero = null;
        this.ultimo = null;
    }
     // ======== Métodos ==========
    
    
    // Lo que pasa aqui es esto
    //primero -> BCP1 -> BCP2 -> BCP3
    //ultimo  -> BCP3

    public void encolar(BCP bcp) {
        bcp.setSiguiente(null);
        if(primero == null){
            primero = bcp;
            ultimo = bcp;
        }else {
            ultimo.setSiguiente(bcp);
            ultimo = bcp;
        }
    }

    public BCP desencolar() {
        if (estaVacia()) {
            return null;
        }
        BCP temp = primero;                     
        primero = primero.getSiguiente();       
        if (primero == null) {
            ultimo = null;                       
        }
        temp.setSiguiente(null);                 
        return temp;                            
    }
    
    public boolean estaVacia() {
        return primero == null;
    }

    public BCP getPrimero() {
        return primero;
    }

    public BCP getUltimo() {
        return ultimo;
    }

    // Correccion - Poll (observación PY1): para algoritmos que no eligen al primero de la cola
    // (SPN, HRRN, prioridad, …) hace falta recorrerla y sacar un proceso de cualquier posición
    // E: no aplica
    // S: List<BCP> - los procesos de la cola, del primero al último (copia; la cola no cambia)
    // R: ninguna
    public java.util.List<BCP> aLista() {
        java.util.List<BCP> lista = new java.util.ArrayList<>();
        for (BCP b = primero; b != null; b = b.getSiguiente()) lista.add(b);
        return lista;
    }

    // E: bcp (BCP) - proceso a sacar de la cola
    // S: boolean - true si estaba en la cola y se sacó
    // R: ninguna
    public boolean quitar(BCP bcp) {
        BCP anterior = null;
        for (BCP b = primero; b != null; anterior = b, b = b.getSiguiente()) {
            if (b != bcp) continue;
            if (anterior == null) primero = b.getSiguiente();
            else anterior.setSiguiente(b.getSiguiente());
            if (ultimo == b) ultimo = anterior;
            b.setSiguiente(null);
            return true;
        }
        return false;
    }

    public int tamanio() {
        int contador = 0;
        BCP actual = primero;
        while (actual != null) {
            contador++;
            actual = actual.getSiguiente();
        }
        return contador;
    }
}
