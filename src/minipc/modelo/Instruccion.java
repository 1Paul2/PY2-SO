package minipc.modelo;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Instruccion: representa una línea del programa .asm ya validada, con su
 * operación y de 0 a 3 operandos guardados como texto. Así el mismo campo
 * sirve para registros ("AX"), números ("5", "-2") o interrupciones ("20H").
 *
 * Ejemplos de texto en memoria (los números quedan tal como se escribieron):
 *   "MOV BX, AX", "MOV BX, 5", "MOV AH, 3CH", "INC", "PARAM 1, 2, 3", "INT 20H"
 */
public class Instruccion {
    private String operacion;
    private String[] operandos;

    // E: operacion (String); operandos (String...) - de 0 a 3 operandos
    // S: no aplica (constructor)
    // R: los operandos null o vacíos se ignoran
    public Instruccion(String operacion, String... operandos){
        this.operacion = operacion;
        List<String> limpios = new ArrayList<>();
        if(operandos != null){
            for(String op : operandos){
                if(op != null && !op.isEmpty()){
                    limpios.add(op);
                }
            }
        }
        this.operandos = limpios.toArray(new String[0]);
    }

    // Constructor anterior, se mantiene para no romper código viejo:
    // new Instruccion("MOV", "AX", 5)  o  new Instruccion("INT", "20H", null)
    public Instruccion(String operacion, String registro, Integer valor){
        this(operacion, registro, valor == null ? null : String.valueOf(valor));
    }

    public String getOperacion(){
        return operacion;
    }

    public String[] getOperandos(){
        return operandos.clone();
    }

    // Correccion - Poll (observación PY1): los operandos se separan con ", " como en el archivo .asm,
    // así la memoria y el disco muestran la instrucción igual que la escribió el programador
    // E: no aplica
    // S: String - la instrucción como texto, p. ej. "MOV AH, 3CH", lista para memoria y disco
    // R: ninguna
    public String getTextoCompleto(){
        if(operandos.length == 0){
            return operacion;
        }
        return operacion + " " + String.join(", ", operandos);
    }

    @Override
    public String toString(){
        return getTextoCompleto();
    }
}