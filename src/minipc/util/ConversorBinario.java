/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package minipc.util;

public class ConversorBinario {

    /**
     * Valida si un texto es uno de los registros válidos (AX, BX, CX, DX, AH, AL).
     */
    public static boolean esRegistroValido(String reg){
        if(reg == null) return false;
        return reg.equals("AX") 
            || reg.equals("BX")
            || reg.equals("CX") 
            || reg.equals("DX")
            || reg.equals("AH") 
            || reg.equals("AL");    
    }

    // Correccion - Poll: conversión única de un operando numérico (decimal o hexadecimal con H),
    // para que el programa guarde el valor tal como se escribió ("3CH") y solo se convierta al ejecutar
    // E: texto (String) - número decimal ("5", "-2", "+3") o hexadecimal terminado en H ("3CH", "0FH")
    // S: int - el valor numérico
    // R: lanza NumberFormatException si el texto no es un número válido
    public static int aEntero(String texto){
        String t = texto.trim().toUpperCase();
        if (t.matches("[0-9][0-9A-F]*H")) {
            return Integer.parseInt(t.substring(0, t.length() - 1), 16);
        }
        return Integer.parseInt(t);
    }

    /**
     * Convierte un texto hexadecimal como "20H" o "1A" a entero.
     * Usado por el parser ASM para interpretar valores como INT 20H.
     */
    public static int HexAEntero(String texto){
        String limpio = texto.trim().toUpperCase();
        if(limpio.endsWith("H")){
            limpio = limpio.substring(0, limpio.length() - 1);
        }
        return Integer.parseInt(limpio, 16);
    }

    public static void main(String[] args){
    }
}
