/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package minipc.util;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import minipc.modelo.algoritmos.SelfishRoundRobin;
import minipc.modelo.BCP;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfiguracionSistema: tamaños de memoria principal, zona del S.O., disco y
 * memoria virtual. Se leen de un archivo de texto (config.txt) para que no
 * queden fijos en el código. Si el archivo no existe, se crea con los valores
 * por defecto (256 / 80 / 512 / 64). Los cuatro tamaños se configuran por separado.
 *
 * Formato de config.txt:
 *   memoriaPrincipal=256
 *   memoriaSistema=80
 *   almacenamientoSecundario=512
 *   memoriaVirtual=64
 */
public class ConfiguracionSistema {

    public static final String ARCHIVO_POR_DEFECTO = "config.txt";

    // Valores predeterminados: memoria principal 256, disco 512 y memoria virtual 64 son los del
    // enunciado. La zona del S.O. la define el diseño:
    // Correccion - Poll (observación PY1): cada BCP ocupa ahora un bloque de BCP.TAMANO_BLOQUE (16)
    // posiciones, así que la zona del S.O. pasa de 64 a 80 para que quepan los 5 BCP
    public static final int MEMORIA_PRINCIPAL_DEF = 256;
    public static final int MEMORIA_SISTEMA_DEF = 5 * BCP.TAMANO_BLOQUE;
    public static final int ALMACENAMIENTO_DEF = 512;
    public static final int MEMORIA_VIRTUAL_DEF = 64;

    private int memoriaPrincipal = MEMORIA_PRINCIPAL_DEF;
    private int memoriaSistema = MEMORIA_SISTEMA_DEF;
    private int almacenamientoSecundario = ALMACENAMIENTO_DEF;
    private int memoriaVirtual = MEMORIA_VIRTUAL_DEF;
    private int cantidadCpus = 1;
    private String algoritmoCpu = "FCFS";
    private int quantumRR = 2;
    private minipc.hardware.EstrategiaMemoria estrategiaMemoria = minipc.hardware.EstrategiaMemoria.FIRST_FIT;
    private int tamanoPagina = 8;
    private int tamanoParticion = 32;
    private String particiones = "16,32,64";
    private int nivelesMLFQ = 3;
    private int periodoBoostMLFQ = 20;
    private int tasaNuevosSRR = SelfishRoundRobin.TASA_NUEVOS_DEF;          // a
    private int tasaAceptadosSRR = SelfishRoundRobin.TASA_ACEPTADOS_DEF;    // b

    // E: no aplica
    // S: no aplica (constructor)
    // R: carga config.txt de la carpeta del proyecto; si no existe, lo crea con los valores por defecto
    public ConfiguracionSistema(){
        File archivo = new File(ARCHIVO_POR_DEFECTO);
        try {
            if (archivo.exists()) {
                cargarDesdeArchivo(archivo);
            } else {
                guardarEnArchivo(archivo);
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudo usar " + ARCHIVO_POR_DEFECTO + ": " + e.getMessage()
                + ". Se usan los valores por defecto.");
        }
    }

    // E: archivo (File) - archivo de configuración clave=valor
    // S: no aplica (void)
    // R: lanza IllegalArgumentException si un valor no es válido
    public void cargarDesdeArchivo(File archivo) throws IOException {
        Properties p = new Properties();
        try (FileReader lector = new FileReader(archivo)) {
            p.load(lector);
        }
        int mp = leerEntero(p, "memoriaPrincipal", memoriaPrincipal);
        int ms = leerEntero(p, "memoriaSistema", memoriaSistema);
        int as = leerEntero(p, "almacenamientoSecundario", almacenamientoSecundario);
        int mv = leerEntero(p, "memoriaVirtual", memoriaVirtual);
        int cpus = leerEntero(p, "cantidadCpus", 1);
        String algoritmo = p.getProperty("algoritmoCpu", "FCFS").trim();
        validarAlgoritmoCpu(algoritmo);
        int quantum = leerEntero(p, "quantumRR", 2);
        validarQuantumRR(quantum);
        int niveles = leerEntero(p, "nivelesMLFQ", 3);
        int boost = leerEntero(p, "periodoBoostMLFQ", 20);
        validarMLFQ(niveles, boost);
        int tasaA = leerEntero(p, "tasaNuevosSRR", SelfishRoundRobin.TASA_NUEVOS_DEF);
        int tasaB = leerEntero(p, "tasaAceptadosSRR", SelfishRoundRobin.TASA_ACEPTADOS_DEF);
        SelfishRoundRobin.validarTasas(tasaA, tasaB);
        validarCantidadCpus(cpus);
        minipc.hardware.EstrategiaMemoria estrategia = minipc.hardware.EstrategiaMemoria.valueOf(
            p.getProperty("estrategiaMemoria", "FIRST_FIT").trim());
        int pagina = leerEntero(p, "tamanoPagina", 8);
        int particion = leerEntero(p, "tamanoParticion", 32);
        String lista = p.getProperty("particiones", "16,32,64");
        setConfiguracionMemoria(mp, ms, as, mv, estrategia, pagina, particion, lista);
        cantidadCpus = cpus;
        quantumRR = quantum;
        nivelesMLFQ = niveles;
        periodoBoostMLFQ = boost;
        tasaNuevosSRR = tasaA;
        tasaAceptadosSRR = tasaB;
        algoritmoCpu = algoritmo;
    }

    // E: archivo (File) - destino
    // S: no aplica (void)
    // R: ninguna
    public void guardarEnArchivo(File archivo) throws IOException {
        Properties p = new Properties();
        p.setProperty("memoriaPrincipal", String.valueOf(memoriaPrincipal));
        p.setProperty("memoriaSistema", String.valueOf(memoriaSistema));
        p.setProperty("almacenamientoSecundario", String.valueOf(almacenamientoSecundario));
        p.setProperty("memoriaVirtual", String.valueOf(memoriaVirtual));
        p.setProperty("cantidadCpus", String.valueOf(cantidadCpus));
        p.setProperty("algoritmoCpu", algoritmoCpu);
        p.setProperty("quantumRR", String.valueOf(quantumRR));
        p.setProperty("estrategiaMemoria", estrategiaMemoria.name());
        p.setProperty("tamanoPagina", String.valueOf(tamanoPagina));
        p.setProperty("tamanoParticion", String.valueOf(tamanoParticion));
        p.setProperty("particiones", particiones);
        p.setProperty("nivelesMLFQ", String.valueOf(nivelesMLFQ));
        p.setProperty("periodoBoostMLFQ", String.valueOf(periodoBoostMLFQ));
        p.setProperty("tasaNuevosSRR", String.valueOf(tasaNuevosSRR));
        p.setProperty("tasaAceptadosSRR", String.valueOf(tasaAceptadosSRR));
        try (FileWriter escritor = new FileWriter(archivo)) {
            p.store(escritor, "Configuracion del Mini PC");
        }
    }

    // Correccion - Poll (observación PY1): la zona del S.O. debe alcanzar al menos para un bloque de BCP
    // (16 posiciones); con menos de 80 caben menos de 5 procesos a la vez
    // E: los cuatro tamaños
    // S: no aplica (void)
    // R: lanza IllegalArgumentException con un mensaje claro si algún valor no es coherente
    public static void validar(int mp, int ms, int as, int mv){
        if (mp < 128 || mp > 4096)
            throw new IllegalArgumentException("La memoria principal debe estar entre 128 y 4096");
        if (ms < BCP.TAMANO_BLOQUE || ms >= mp)
            throw new IllegalArgumentException("La zona del S.O. debe ser al menos " + BCP.TAMANO_BLOQUE
                + " (un BCP ocupa " + BCP.TAMANO_BLOQUE + " posiciones; para 5 procesos hacen falta "
                + (5 * BCP.TAMANO_BLOQUE) + ") y menor que la memoria principal");
        if (as < 128 || as > 8192)
            throw new IllegalArgumentException("El disco debe estar entre 128 y 8192");
        if (mv < 0)
            throw new IllegalArgumentException("La memoria virtual no puede ser negativa");
        int indice = Math.max(10, as / 16);   // mismo tamaño de índice que usa Disco
        if (mv >= as - indice)
            throw new IllegalArgumentException("La memoria virtual debe ser menor que " + (as - indice)
                + " para dejar espacio en el disco al índice (" + indice + ") y a los archivos");
    }

    private int leerEntero(Properties p, String clave, int porDefecto){
        String v = p.getProperty(clave);
        if (v == null) return porDefecto;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El valor de \"" + clave + "\" no es un número: " + v);
        }
    }

    public static void validarCantidadCpus(int cantidad){
        if (cantidad < 1 || cantidad > 5)
            throw new IllegalArgumentException("La cantidad de CPU debe estar entre 1 y 5");
    }
    private static void validarAlgoritmoCpu(String algoritmo){
        if (!"FCFS".equals(algoritmo) && !"SRT".equals(algoritmo) && !"SPN".equals(algoritmo) && !"RR".equals(algoritmo) && !"HRRN".equals(algoritmo) && !"SRR".equals(algoritmo) && !"Lottery".equals(algoritmo) && !"Prioridad".equals(algoritmo)
            && !"Guaranteed".equals(algoritmo) && !"MLFQ".equals(algoritmo))
            throw new IllegalArgumentException("Algoritmo de CPU inválido: " + algoritmo);
    }
    public String getAlgoritmoCpu(){ return algoritmoCpu; }
    public void setAlgoritmoCpu(String algoritmo){
        validarAlgoritmoCpu(algoritmo);
        algoritmoCpu = algoritmo;
    }

    private static void validarQuantumRR(int quantum){
        if (quantum < 1) throw new IllegalArgumentException("El quantum debe ser positivo");
    }
    public int getQuantumRR(){ return quantumRR; }
    public void setQuantumRR(int quantum){
        validarQuantumRR(quantum);
        quantumRR = quantum;
    }

    private static void validarMLFQ(int niveles, int boost){
        if (niveles < 2 || niveles > 8 || boost < 1)
            throw new IllegalArgumentException("MLFQ requiere 2 a 8 niveles y promoción positiva");
    }
    public int getNivelesMLFQ(){ return nivelesMLFQ; }
    public int getPeriodoBoostMLFQ(){ return periodoBoostMLFQ; }
    public void setParametrosMLFQ(int niveles, int boost){
        validarMLFQ(niveles, boost);
        nivelesMLFQ = niveles;
        periodoBoostMLFQ = boost;
    }

    public int getTasaNuevosSRR(){ return tasaNuevosSRR; }
    public int getTasaAceptadosSRR(){ return tasaAceptadosSRR; }

    // E: tasaNuevos (int) - a; tasaAceptados (int) - b
    // S: no aplica (void)
    // R: a >= 1 y b >= 0
    public void setParametrosSRR(int tasaNuevos, int tasaAceptados){
        SelfishRoundRobin.validarTasas(tasaNuevos, tasaAceptados);
        tasaNuevosSRR = tasaNuevos;
        tasaAceptadosSRR = tasaAceptados;
    }

    public int getCantidadCpus(){ return cantidadCpus; }
    public void setCantidadCpus(int cantidad){
        validarCantidadCpus(cantidad);
        cantidadCpus = cantidad;
    }

    private static int[] leerParticiones(String lista){
        if (lista == null || lista.isBlank()) throw new IllegalArgumentException("Ingrese tamaños de particiones separados por comas");
        String[] partes = lista.split(",", -1);
        int[] valores = new int[partes.length];
        try {
            for (int i = 0; i < partes.length; i++) {
                valores[i] = Integer.parseInt(partes[i].trim());
                if (valores[i] < 1 || valores[i] > 4096) throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cada partición debe tener entre 1 y 4096 posiciones");
        }
        return valores;
    }

    private static void validarAsignacion(int usuario, minipc.hardware.EstrategiaMemoria estrategia,
                                         int pagina, int particion, String lista){
        java.util.Objects.requireNonNull(estrategia, "Seleccione una estrategia de memoria");
        if (pagina < 1 || pagina > 4096 || particion < 1 || particion > 4096)
            throw new IllegalArgumentException("Página y partición deben tener entre 1 y 4096 posiciones");
        int[] tamanos = leerParticiones(lista);
        if (estrategia.esPaginada() && pagina > usuario)
            throw new IllegalArgumentException("Debe caber al menos una página en la RAM de usuario");
        if (estrategia == minipc.hardware.EstrategiaMemoria.FIJA_IGUAL && particion > usuario)
            throw new IllegalArgumentException("Debe caber al menos una partición en la RAM de usuario");
        long suma = java.util.Arrays.stream(tamanos).asLongStream().sum();
        if (estrategia == minipc.hardware.EstrategiaMemoria.FIJA_VARIABLE && suma > usuario)
            throw new IllegalArgumentException("Las particiones suman " + suma + " y la RAM de usuario tiene " + usuario);
    }

    public void setConfiguracionMemoria(int mp, int ms, int as, int mv,
            minipc.hardware.EstrategiaMemoria estrategia, int pagina, int particion, String lista){
        validar(mp, ms, as, mv);
        validarAsignacion(mp - ms, estrategia, pagina, particion, lista);
        memoriaPrincipal = mp;
        memoriaSistema = ms;
        almacenamientoSecundario = as;
        memoriaVirtual = mv;
        estrategiaMemoria = estrategia;
        tamanoPagina = pagina;
        tamanoParticion = particion;
        particiones = lista.trim();
    }

    public minipc.hardware.EstrategiaMemoria getEstrategiaMemoria(){ return estrategiaMemoria; }
    public int getTamanoPagina(){ return tamanoPagina; }
    public int getTamanoParticion(){ return tamanoParticion; }
    public String getListaParticiones(){ return particiones; }
    public int[] getParticiones(){ return leerParticiones(particiones); }

    public int getMemoriaPrincipal(){
        return memoriaPrincipal; 
    }
    public int getMemoriaSistema(){
        return memoriaSistema;
    }
    public int getAlmacenamientoSecundario(){
        return almacenamientoSecundario; 
    }
    public int getMemoriaVirtual(){
        return memoriaVirtual;
    }

    // E: mp (int) - memoria principal; ms (int) - zona del S.O.; as (int) - disco; mv (int) - memoria virtual
    // S: no aplica (void)
    // R: valida todo antes de asignar
    public void setTamanos(int mp, int ms, int as, int mv){
        validar(mp, ms, as, mv);
        validarAsignacion(mp - ms, estrategiaMemoria, tamanoPagina, tamanoParticion, particiones);
        memoriaPrincipal = mp;
        memoriaSistema = ms;
        almacenamientoSecundario = as;
        memoriaVirtual = mv;
    }
}
