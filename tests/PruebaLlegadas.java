import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import minipc.modelo.*;
import minipc.modelo.algoritmos.AlgoritmoPlanificacion;
import minipc.util.ParserASM;

public class PruebaLlegadas {
    static List<Instruccion> programa(String... lineas) {
        ArrayList<Instruccion> resultado = new ArrayList<>();
        for (String linea : lineas) resultado.add(ParserASM.procesarLinea(linea));
        return resultado;
    }
    static GestorProcesos gestor() { return new GestorProcesos(256, 80, 512, 64); }
    static void verificar(boolean valor, String mensaje) {
        if (!valor) throw new AssertionError(mensaje);
    }

    // Algoritmo de prueba: expropia cada 2 s y anota cuántos preparados ve en cada decisión
    static class Expropiador implements AlgoritmoPlanificacion {
        final List<Integer> vistos = new ArrayList<>();
        public String getNombre() { return "Prueba"; }
        public BCP seleccionar(ColaTrabajo preparados) { return preparados.desencolar(); }
        @Override public boolean debeExpropiar(BCP actual, int segundos, ColaTrabajo preparados) {
            vistos.add(preparados.tamanio());
            return segundos >= 2;
        }
    }

    public static void main(String[] args) throws IOException {
        // Llegadas futuras, CPU ociosa y empates
        GestorProcesos g = gestor();
        BCP tarde = g.cargarPrograma(programa("INT 20H"), "tarde.asm", 5);
        BCP temprano = g.cargarPrograma(programa("INT 20H"), "temprano.asm", 2);
        BCP empate = g.cargarPrograma(programa("INT 20H"), "empate.asm", 2);
        verificar(g.getReloj() == 0 && g.procesosEnMemoria() == 0, "No admitir antes de llegada");
        verificar(g.getPendientesLlegada().size() == 3, "Pendientes visibles para la GUI");
        verificar(g.getPlanificador().getListaTrabajos().estaVacia(), "Pendientes fuera de colas");
        verificar(g.ejecutarUnPaso() && g.getReloj() == 1, "Avance ocioso");
        verificar(g.procesosEnMemoria() == 0, "Sin RAM antes de llegada");
        g.ejecutarUnPaso();
        verificar(g.getReloj() == 2 && g.getCpu().getBcpActual() == temprano, "Admisión exacta y orden real");
        verificar(temprano.getTiempoInicio() == 2 && temprano.getTiempoEmpleado() == 0, "No cobrar CPU durante espera");
        verificar(tarde.getDireccionBCP() == -1, "Futuro sin BCP en RAM");
        g.ejecutarTodo();
        verificar(temprano.getTiempoInicio() <= empate.getTiempoInicio(), "Empates estables");
        verificar(tarde.getTiempoInicio() == 5, "Inicio en su llegada");
        verificar(!g.hayProcesos(), "Todos finalizados");
        verificar(!g.ejecutarUnPaso(), "No avanzar sin procesos");

        // FCFS no desaloja, y una llegada ya cumplida no se puede reconfigurar
        g = gestor();
        BCP primero = g.cargarPrograma(programa("MOV AX, 1", "MOV BX, 2", "INT 20H"), "primero.asm", 0);
        BCP segundo = g.cargarPrograma(programa("INT 20H"), "segundo.asm", 1);
        g.ejecutarUnPaso();
        verificar(primero.getTiempoInicio() == 0, "Llegada en cero");
        verificar(g.getCpu().getBcpActual() == primero && segundo.getDireccionBCP() >= 0, "Admitir durante CPU ocupada sin desalojar FCFS");
        try { g.configurarLlegada(segundo, 8); throw new AssertionError("Reconfigurar admitido"); }
        catch (IllegalStateException esperado) { }
        try { g.cargarPrograma(programa("INT 20H"), "invalido.asm", -1); throw new AssertionError("Llegada negativa"); }
        catch (IllegalArgumentException esperado) { }
        g.ejecutarTodo();

        // Una llegada pendiente sí se puede reconfigurar
        g = gestor();
        BCP movido = g.cargarPrograma(programa("INT 20H"), "movido.asm", 9);
        g.configurarLlegada(movido, 3);
        g.ejecutarTodo();
        verificar(movido.getTiempoInicio() == 3, "Reconfigurar pendiente");

        // Ráfaga = líneas a procesar: sin comentarios ni vacías, e igual al tiempo de CPU
        File archivo = File.createTempFile("rafaga", ".asm");
        archivo.deleteOnExit();
        Files.write(archivo.toPath(), Arrays.asList("; comentario", "", "MOV AX, 1", "  ", "MOV BX, 2 ; otro", "; fin", "INT 20H", ""));
        g = gestor();
        BCP comentado = g.cargarPrograma(ParserASM.leerArchivo(archivo), archivo.getName(), 0);
        verificar(comentado.getRafaga() == 3, "Ráfaga sin comentarios ni vacías");
        verificar(comentado.getRafaga() == comentado.getTamanioProceso(), "Una línea = una posición de memoria");
        g.ejecutarTodo();
        verificar(comentado.getTiempoEmpleado() == 3 && comentado.getTiempoFin() == 3, "1 segundo por instrucción");

        // El BCP sigue cabiendo en su bloque y guarda ráfaga y llegada
        String[] celdas = comentado.aCeldasMemoria();
        verificar(celdas.length == BCP.TAMANO_BLOQUE, "BCP dentro de su bloque");
        verificar(String.join("|", celdas).contains("raf=3 lleg=0"), "Ráfaga y llegada en el BCP");

        // Las llegadas del segundo actual se ven antes de decidir la expropiación
        g = gestor();
        Expropiador algoritmo = new Expropiador();
        g.getPlanificador().setAlgoritmo(algoritmo);
        BCP largo = g.cargarPrograma(programa("MOV AX, 1", "MOV BX, 2", "MOV CX, 3", "INT 20H"), "largo.asm", 0);
        BCP nuevo = g.cargarPrograma(programa("MOV AX, 1", "INT 20H"), "nuevo.asm", 2);
        g.ejecutarUnPaso();
        g.ejecutarUnPaso();
        verificar(algoritmo.vistos.get(1) == 1, "La llegada del segundo 2 se ve al decidir");
        verificar(g.getCpu().getBcpActual() == nuevo, "La llegada va delante del expropiado");
        g.ejecutarTodo();
        verificar("Finalizado".equals(largo.getEstado()) && "Finalizado".equals(nuevo.getEstado()), "Expropiados terminan");
        verificar(largo.getTiempoEmpleado() == 4 && nuevo.getTiempoEmpleado() == 2, "Ráfagas completas tras expropiar");

        System.out.println("OK: llegadas, ociosidad, empates, ráfaga, BCP y expropiación");
    }
}
