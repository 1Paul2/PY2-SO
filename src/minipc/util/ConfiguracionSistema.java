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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfiguracionSistema: tamaños de memoria principal, zona del S.O., disco y
 * memoria virtual. Se leen de un archivo de texto (config.txt) para que no
 * queden fijos en el código. Si el archivo no existe, se crea con los valores
 * por defecto del enunciado (256 / 64 / 512 / 64). Los cuatro tamaños se
 * configuran por separado.
 *
 * Formato de config.txt:
 *   memoriaPrincipal=256
 *   memoriaSistema=64           
 *   almacenamientoSecundario=512
 *   memoriaVirtual=64            
 */
public class ConfiguracionSistema {

    public static final String ARCHIVO_POR_DEFECTO = "config.txt";

    // Valores predeterminados del enunciado
    public static final int MEMORIA_PRINCIPAL_DEF = 256;
    public static final int MEMORIA_SISTEMA_DEF = 64;
    public static final int ALMACENAMIENTO_DEF = 512;
    public static final int MEMORIA_VIRTUAL_DEF = 64;

    private int memoriaPrincipal = MEMORIA_PRINCIPAL_DEF;
    private int memoriaSistema = MEMORIA_SISTEMA_DEF;
    private int almacenamientoSecundario = ALMACENAMIENTO_DEF;
    private int memoriaVirtual = MEMORIA_VIRTUAL_DEF;

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

    // Correccion - Poll: lee también la zona del S.O. y la memoria virtual del archivo, en vez de
    // calcularlas con porcentajes fijos
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
        setTamanos(mp, ms, as, mv);
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
        try (FileWriter escritor = new FileWriter(archivo)) {
            p.store(escritor, "Configuracion del Mini PC");
        }
    }

    // Correccion - Poll: como la memoria virtual ya no se calcula, se valida que deje espacio en el
    // disco para el índice y los archivos
    // E: los cuatro tamaños
    // S: no aplica (void)
    // R: lanza IllegalArgumentException con un mensaje claro si algún valor no es coherente
    public static void validar(int mp, int ms, int as, int mv){
        if (mp < 128 || mp > 4096)
            throw new IllegalArgumentException("La memoria principal debe estar entre 128 y 4096");
        if (ms < 5 || ms >= mp)
            throw new IllegalArgumentException("La zona del S.O. debe ser al menos 5 (para los BCP) y menor que la memoria principal");
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

    // Correccion - Poll: recibe los cuatro tamaños; la zona del S.O. y la memoria virtual ya no
    // se calculan con porcentajes fijos (25 % y 12,5 %)
    // E: mp (int) - memoria principal; ms (int) - zona del S.O.; as (int) - disco; mv (int) - memoria virtual
    // S: no aplica (void)
    // R: valida todo antes de asignar
    public void setTamanos(int mp, int ms, int as, int mv){
        validar(mp, ms, as, mv);
        memoriaPrincipal = mp;
        memoriaSistema = ms;
        almacenamientoSecundario = as;
        memoriaVirtual = mv;
    }
}
