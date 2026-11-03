import java.util.Arrays;
import java.util.List;
import minipc.modelo.*;
import minipc.util.ParserASM;

public class PruebaLlegadas {
    static List<Instruccion> programa(String... lineas) {
        java.util.ArrayList<Instruccion> resultado = new java.util.ArrayList<>();
        for (String linea : lineas) resultado.add(ParserASM.procesarLinea(linea));
        return resultado;
    }
    static GestorProcesos gestor() { return new GestorProcesos(256, 64, 512, 64); }
    static void verificar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
    }
    public static void main(String[] args) {
        GestorProcesos g = gestor();
        BCP tarde = g.cargarPrograma(programa("INT 20H"), "tarde.asm", 4, 5);
        BCP temprano = g.cargarPrograma(programa("INT 20H"), "temprano.asm", 1, 2);
        BCP empate = g.cargarPrograma(programa("INT 20H"), "empate.asm", 1, 2);
        verificar(g.getReloj() == 0 && g.procesosEnMemoria() == 0, "No admitir antes de llegada");
        verificar(g.getPlanificador().getListaTrabajos().estaVacia(), "Pendientes fuera de colas");
        verificar(g.ejecutarUnPaso() && g.getReloj() == 1, "Avance ocioso");
        verificar(g.procesosEnMemoria() == 0, "Sin RAM antes de llegada");
        g.ejecutarUnPaso();
        verificar(g.getReloj() == 2 && g.getCpu().getBcpActual() == temprano, "Admisión exacta y orden real");
        verificar(temprano.getTiempoInicio() == 2 && temprano.getTiempoEmpleado() == 0, "No cobrar CPU durante espera");
        verificar(tarde.getDireccionBCP() == -1, "Futuro sin BCP en RAM");
        g.ejecutarTodo();
        verificar(temprano.getTiempoInicio() <= empate.getTiempoInicio(), "Empates estables");
        verificar(tarde.getTiempoInicio() >= 5 && tarde.getRafaga() == 4, "Ráfaga independiente del tamaño ejecutable");
        verificar(!g.hayProcesos(), "Todos finalizados");
        verificar(!g.ejecutarUnPaso(), "No avanzar sin procesos");

        g = gestor();
        BCP primero = g.cargarPrograma(programa("MOV AX, 1", "MOV BX, 2", "INT 20H"), "primero.asm", 3, 0);
        BCP segundo = g.cargarPrograma(programa("INT 20H"), "segundo.asm", 1, 1);
        g.ejecutarUnPaso();
        verificar(primero.getTiempoInicio() == 0, "Llegada en cero");
        verificar(g.getCpu().getBcpActual() == primero && segundo.getDireccionBCP() >= 0, "Admitir durante CPU ocupada sin desalojar FCFS");
        try { g.configurarLlegada(segundo, 8); throw new AssertionError("Reconfigurar admitido"); }
        catch (IllegalStateException esperado) { }
        try { g.cargarPrograma(programa("INT 20H"), "invalido.asm", 1, -1); throw new AssertionError("Llegada negativa"); }
        catch (IllegalArgumentException esperado) { }
        g.ejecutarTodo();

        g = gestor();
        BCP bloqueado = g.cargarPrograma(programa("INT 09H", "INT 20H"), "teclado.asm", 2, 0);
        BCP futuro = g.cargarPrograma(programa("INT 20H"), "futuro.asm", 1, 10);
        g.ejecutarTodo();
        verificar("Finalizado".equals(futuro.getEstado()) && futuro.getTiempoInicio() == 10, "Teclado no impide futuras llegadas");
        verificar(g.bloqueadoPorTeclado(), "Espera teclado sin futuros");
        g.ingresarTeclado(7);
        g.ejecutarTodo();
        verificar(!g.hayProcesos(), "Reanudar teclado");
        System.out.println("OK: llegadas, ociosidad, empates, FCFS, ráfaga y teclado");
    }
}
