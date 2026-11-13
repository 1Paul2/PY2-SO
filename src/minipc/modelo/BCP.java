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

import minipc.hardware.CPU;
import java.util.List;
import java.util.ArrayList;

/**
 * BCP (Bloque de Control de Proceso): representa el estado de un proceso en
 * un momento dado - su identificador, estado, y una copia de los registros
 * de la CPU capturados al momento de invocar capturaEstado().
 */
public class BCP {
    // Modelo de 5 estados (Stallings, cap. 3): Nuevo, Preparado, Ejecución, En espera y Finalizado.
    // En este simulador ningún proceso se bloquea (no hay entrada de teclado y las interrupciones
    // duran 1 s), así que "En espera" queda como parte del modelo pero no se usa.
    // Correccion - Poll (observación PY1): un proceso que termina por un error (pila, salto,
    // acceso a memoria) se distingue del que terminó bien con INT 20H o al llegar al final
    public static final String FINALIZADO = "Finalizado";
    public static final String FINALIZADO_ERROR = "Finalizado con error";

    // Correccion - Poll (observación PY1): el BCP se guarda en la zona del S.O. campo por campo,
    // un campo por posición de memoria; este es el tamaño del bloque que ocupa cada BCP
    public static final String[] CAMPOS_MEMORIA = {
        "Estado", "PC", "IR", "AC", "AX", "BX", "CX", "DX",
        "AH/AL/CMP", "Pila", "Contable", "Archivos", "Base", "Alcance", "Prioridad", "Sig BCP"
    };
    public static final int TAMANO_BLOQUE = CAMPOS_MEMORIA.length;   // 16 posiciones

    private int idProceso;
    private String estado;
    private int pcGuardado;
    private int acGuardado;
    private int axGuardado;
    private int bxGuardado;
    private int cxGuardado;
    private int dxGuardado;
    private String irGuardado;
    private int[] pila;
    private int topePila;
    private int tiempoInicio;
    private int tiempoEmpleado;
    private int cpuId;
    private List<String> archivosAbiertos;
    private BCP siguiente;
    private int direccionBase;
    private int tamanioProceso;
    private int prioridad;
    private int boletos = 1;
    private boolean flagGuardado;
    private int tiempoFin;
    private int rafaga;
    private int tiempoLlegada;
    private int direccionBCP;
    private int ahGuardado;           // registro AH (función de INT 21H)
    private int alGuardado;            // registro AL (dato que se lee/escribe con INT 21H)
    private String dxTextoGuardado;    // nombre de archivo guardado en DX (MOV DX, "archivo.txt")
    private java.util.Map<String, Integer> punterosLectura;   // próxima línea a leer de cada archivo abierto
            
        // E: idProceso (int) - identificador del proceso; estado (String)
        // S: no aplica (constructor)
        // R: ninguna
        public BCP(int idProceso, String estado){
            this.idProceso = idProceso;
            this.estado = estado;
            this.pcGuardado = 0;
            this.acGuardado = 0;
            this.axGuardado = 0;
            this.bxGuardado = 0;
            this.cxGuardado = 0;
            this.dxGuardado = 0;
            this.irGuardado = "";
            this.pila = new int[5]; 
            this.topePila = -1;
            this.tiempoInicio = -1;
            this.tiempoFin = -1;
            this.flagGuardado = false;
            this.direccionBCP = -1;
            this.ahGuardado = 0;
            this.alGuardado = 0;
            this.dxTextoGuardado = null;
            this.punterosLectura = new java.util.HashMap<>();
            this.tiempoEmpleado = 0;
            this.cpuId = -1;
            this.archivosAbiertos = new ArrayList<>();
            this.siguiente = null;
            this.direccionBase = -1;
            this.tamanioProceso = 0;
            this.prioridad = 0;  
        }
        
        // E: cpu (CPU) - la CPU de la que se va a copiar el estado actual
        // S: no aplica (void)
        // R: ninguna
        public void capturaEstado(CPU cpu){
            this.pcGuardado = cpu.getPC();
            this.acGuardado = cpu.getAC();
            this.axGuardado = cpu.getAX();
            this.bxGuardado = cpu.getBX();
            this.cxGuardado = cpu.getCX();
            this.dxGuardado = cpu.getDX();
            this.irGuardado = cpu.getIR();
            this.flagGuardado = cpu.isFlagIgual();
            this.ahGuardado = cpu.getAH();
            this.alGuardado = cpu.getAL();
            this.dxTextoGuardado = cpu.getDXTexto();
        }
        
        public void restaurarEstado(CPU cpu){
            cpu.setPC(this.pcGuardado);
            cpu.setAC(this.acGuardado);
            cpu.setAX(this.axGuardado);
            cpu.setBX(this.bxGuardado);
            cpu.setCX(this.cxGuardado);
            cpu.setDX(this.dxGuardado);
            cpu.setIR(this.irGuardado);
            cpu.setFlagIgual(this.flagGuardado);
            cpu.setAH(this.ahGuardado);
            cpu.setAL(this.alGuardado);
            cpu.setDXTexto(this.dxTextoGuardado);
        }
        
        public void setEstado(String nuevoEstado){
            this.estado = nuevoEstado;
        }
        
        public void push(int valor) {
            if (topePila < 4) {
                topePila++;
                pila[topePila] = valor;
            } else {
                throw new RuntimeException("Desbordamiento de pila en el proceso " + idProceso);
            }
        }
        public int pop(){
            if(topePila >= 0){
                int valor = pila[topePila];
                topePila--;
                return valor;
            }else{ 
                throw new RuntimeException("Pila vacía en el proceso " + idProceso);
            }
        }
        
        public int getIdProceso(){
            return idProceso;
        }

        public String getEstado(){
            return estado;
        }

        // Correccion - Poll (observación PY1): "Finalizado" y "Finalizado con error" cuentan los dos
        // como terminados; antes solo existía "Finalizado"
        // E: no aplica
        // S: boolean - true si el proceso ya terminó, bien o con error
        // R: ninguna
        public boolean estaFinalizado(){
            return FINALIZADO.equals(estado) || FINALIZADO_ERROR.equals(estado);
        }

        public int getPcGuardado(){
            return pcGuardado;
        }

        public int getAcGuardado(){
            return acGuardado;
        }

        public int getAxGuardado(){
            return axGuardado;
        }

        public int getBxGuardado(){
            return bxGuardado;
        }

        public int getCxGuardado(){
            return cxGuardado;
        }

        public int getDxGuardado(){
            return dxGuardado;
        }
        public String getIrGuardado(){
            return irGuardado;
        }

        public int getTiempoInicio(){
            return tiempoInicio;
        }

        public int getTiempoEmpleado(){
            return tiempoEmpleado;
        }

        public int getCpuId(){
            return cpuId;
        }

        public List<String> getArchivosAbiertos(){
            return archivosAbiertos;
        }

        public BCP getSiguiente(){
            return siguiente;
        }

        public int getDireccionBase(){
            return direccionBase;
        }

        public int getTamanioProceso(){
            return tamanioProceso;
        }

        public int getPrioridad(){
            return prioridad;
        }

        public void setPcGuardado(int pc){
            this.pcGuardado = pc;
        }

        public void setAcGuardado(int ac){
            this.acGuardado = ac;
        }

        public void setAxGuardado(int ax){
            this.axGuardado = ax;
        }

        public void setBxGuardado(int bx){
            this.bxGuardado = bx;
        }

        public void setCxGuardado(int cx){
            this.cxGuardado = cx;
        }

        public void setDxGuardado(int dx){
            this.dxGuardado = dx;
        }

        public void setIrGuardado(String ir){
            this.irGuardado = ir;
        }

        public void setTiempoInicio(int t){
            this.tiempoInicio = t;
        }

        public void setTiempoEmpleado(int t){
            this.tiempoEmpleado = t;
        }

        public void setCpuId(int id){
            this.cpuId = id;
        }

        public void setSiguiente(BCP sig){
            this.siguiente = sig;
        }

        public void setDireccionBase(int dir){
            this.direccionBase = dir;
        }

        public void setTamanioProceso(int tam){
            this.tamanioProceso = tam;
        }

        public void setPrioridad(int p){
            this.prioridad = p;
        }

        public void agregarArchivoAbierto(String archivo){
            this.archivosAbiertos.add(archivo);
        }
        public int getTopePila() {
            return topePila;
        }
        

        public int getBoletos(){ return boletos; }
        public void setBoletos(int cantidad){
            if (cantidad < 1) throw new IllegalArgumentException("Los boletos deben ser positivos");
            boletos = cantidad;
        }

        public int getRafaga(){ return rafaga; }
        public void setRafaga(int valor){
            if (valor < 0) throw new IllegalArgumentException("Ráfaga negativa");
            rafaga = valor;
        }
        public int getTiempoLlegada(){ return tiempoLlegada; }
        public void setTiempoLlegada(int valor){
            if (valor < 0) throw new IllegalArgumentException("Llegada negativa");
            tiempoLlegada = valor;
        }

        public int getTiempoFin(){ return tiempoFin; }
        public void setTiempoFin(int t){ this.tiempoFin = t; }

        // Primera posición del bloque de la zona del S.O. donde está guardado este BCP (-1 si no está en memoria)
        public int getDireccionBCP(){ return direccionBCP; }
        public void setDireccionBCP(int dir){ this.direccionBCP = dir; }

        // E: no aplica
        // S: String - contenido de la pila, del fondo al tope, p. ej. "[1,2,3]"
        // R: ninguna
        public String pilaTexto(){
            StringBuilder sb = new StringBuilder("[");
            for(int i = 0; i <= topePila; i++){
                if(i > 0) sb.append(",");
                sb.append(pila[i]);
            }
            return sb.append("]").toString();
        }

        // Correccion - Poll (observación PY1): reemplaza a aTextoMemoria(), que juntaba todo el BCP en
        // un solo texto para una sola posición. Ahora cada elemento del BCP tiene su propia posición
        // en la zona del S.O., en el orden de CAMPOS_MEMORIA
        // E: no aplica
        // S: String[] - TAMANO_BLOQUE valores, uno por posición del bloque del BCP.
        //    "Sig BCP" es la DIRECCIÓN de memoria donde empieza el bloque del siguiente BCP de su cola
        //    (-1 si es el último)
        // R: ninguna
        public String[] aCeldasMemoria(){
            int sig = (siguiente != null) ? siguiente.getDireccionBCP() : -1;
            String dx = dxTextoGuardado != null ? "\"" + dxTextoGuardado + "\"" : String.valueOf(dxGuardado);
            String archivos = archivosAbiertos.isEmpty() ? "-" : String.join(", ", archivosAbiertos);
            return new String[]{
                estado,
                "PC=" + pcGuardado,
                "IR=" + (irGuardado == null || irGuardado.isEmpty() ? "-" : irGuardado),
                "AC=" + acGuardado,
                "AX=" + axGuardado,
                "BX=" + bxGuardado,
                "CX=" + cxGuardado,
                "DX=" + dx,
                "AH=" + String.format("%02XH", ahGuardado) + " AL=" + alGuardado + " CMP=" + (flagGuardado ? "1" : "0"),
                "Pila=" + pilaTexto() + " SP=" + topePila,
                "CPU=" + (cpuId < 0 ? "-" : cpuId) + " ini=" + (tiempoInicio < 0 ? "-" : tiempoInicio)
                    + " emp=" + tiempoEmpleado + " raf=" + rafaga + " lleg=" + tiempoLlegada,
                "Archivos=" + archivos,
                "Base=" + direccionBase,
                "Alcance=" + tamanioProceso,
                "Prioridad=" + prioridad,
                "Sig=" + sig
            };
        }

        // ---- INT 21H y suspensión ----
        public int getAhGuardado(){
            return ahGuardado; 
        }
        public int getAlGuardado(){
            return alGuardado;
        }
        public String getDxTextoGuardado(){
            return dxTextoGuardado; 
        }
        public void setDxTextoGuardado(String t){
            this.dxTextoGuardado = t; 
        }

        // E: archivo (String) - nombre del archivo
        // S: boolean - true si el proceso tiene ese archivo abierto
        // R: el primer archivo de la lista es el propio programa (.asm), no cuenta como abierto por INT 21H
        public boolean tieneAbierto(String archivo){
            return archivosAbiertos.indexOf(archivo) > 0;
        }

        // Abre (o reabre) un archivo para INT 21H: lo agrega a la lista y reinicia su lectura
        public void abrirArchivo(String archivo){
            if (!tieneAbierto(archivo)) archivosAbiertos.add(archivo);
            punterosLectura.put(archivo, 0);
        }

        public void cerrarArchivo(String archivo){
            int i = archivosAbiertos.indexOf(archivo);
            if (i > 0) archivosAbiertos.remove(i);
            punterosLectura.remove(archivo);
        }

        public int getPunteroLectura(String archivo){
            return punterosLectura.getOrDefault(archivo, 0);
        }

        public void setPunteroLectura(String archivo, int p){
            punterosLectura.put(archivo, p);
        }
}
