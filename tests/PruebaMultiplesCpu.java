import java.util.*;
import java.nio.file.*;
import minipc.modelo.*;
import minipc.hardware.CPU;
import minipc.util.*;
import minipc.modelo.algoritmos.AlgoritmoPlanificacion;

public class PruebaMultiplesCpu {
    static void exigir(boolean ok, String mensaje) {
        if (!ok) throw new AssertionError(mensaje);
    }
    static List<Instruccion> programa(String... lineas) {
        List<Instruccion> p = new ArrayList<>();
        for (String l : lineas) p.add(ParserASM.procesarLinea(l));
        return p;
    }
    static GestorProcesos gestor(int n) { return new GestorProcesos(1024, 400, 2048, 64, n); }
    static void invariantes(GestorProcesos g) {
        Set<BCP> vistos = new HashSet<>();
        int ocupadas = 0;
        for (int i = 0; i < g.getCantidadCpus(); i++) {
            BCP b = g.getCpu(i).getBcpActual();
            if (b != null) {
                exigir(vistos.add(b), "Un proceso ejecuta en dos CPU");
                exigir(b.getCpuId() == i + 1 && "Ejecución".equals(b.getEstado()), "CPU/estado incoherente");
                ocupadas++;
            }
        }
        exigir(ocupadas == g.getCpusOcupadas(), "Conteo CPU");
        for (ColaTrabajo cola : new ColaTrabajo[]{g.getPlanificador().getListaTrabajos(),
                g.getPlanificador().getListaProcesos(), g.getPlanificador().getListaEspera(),
                g.getPlanificador().getListaFinalizados()}) {
            int limite = 0;
            for (BCP b = cola.getPrimero(); b != null; b = b.getSiguiente()) {
                exigir(++limite <= g.getTodosLosProcesos().size(), "Ciclo en cola");
                exigir(vistos.add(b), "Proceso duplicado en cola");
            }
        }
        Set<Integer> posiciones = new HashSet<>();
        int residentes = 0;
        for (BCP b : g.getTodosLosProcesos()) {
            if (b.getTiempoLlegada() > g.getReloj()) {
                exigir(b.getDireccionBCP() < 0 && b.getTiempoEmpleado() == 0, "Admisión anticipada");
            }
            if (b.getDireccionBCP() < 0) continue;
            residentes++;
            for (int j = 0; j < BCP.TAMANO_BLOQUE; j++)
                exigir(posiciones.add(b.getDireccionBCP() + j), "BCP solapados");
            for (int j = 0; j < b.getTamanioProceso(); j++)
                exigir(posiciones.add(b.getDireccionBase() + j), "Programas solapados");
            exigir(String.join("|", b.aCeldasMemoria()).contains("CPU=" + b.getCpuId())
                || b.getCpuId() < 1, "CPU guardada en BCP");
        }
        exigir(residentes == g.procesosEnMemoria(), "Conteo de residentes");
        exigir(residentes <= 5 * g.getCantidadCpus(), "Más de cinco procesos por CPU");
    }
    public static void main(String[] args) throws Exception {
        for (int n : new int[]{0, 6}) {
            try { gestor(n); throw new AssertionError("Cantidad inválida aceptada"); }
            catch (IllegalArgumentException esperado) { }
        }
        int escenarios = 0;
        for (int n = 1; n <= 5; n++) {
            GestorProcesos g = gestor(n);
            for (int i = 0; i < n; i++) g.cargarPrograma(programa("MOV AX, " + i, "INC AX", "INT 20H"), "p" + i + ".asm", 0);
            g.ejecutarUnPaso();
            exigir(g.getReloj() == 1 && g.getCpusOcupadas() == n, "Reloj global y todas ocupadas");
            for (int i = 0; i < n; i++) exigir(g.getCpu(i).getAX() == i, "Registros independientes");
            invariantes(g);
            g.ejecutarTodo();
            exigir(g.getReloj() == 3, "N CPU no multiplican el reloj");
            for (BCP b : g.getTodosLosProcesos()) {
                exigir(b.getTiempoInicio() == 0 && b.getTiempoFin() == 3 && b.getTiempoEmpleado() == 3, "Tiempos paralelos");
                exigir(b.getAxGuardado() == b.getIdProceso(), "Contexto final independiente");
            }
            escenarios++;
        }
        GestorProcesos g = gestor(5);
        for (int i = 0; i < 30; i++) g.cargarPrograma(programa("INC AX", "INT 20H"), "grupo.asm", 0);
        g.ejecutarUnPaso();
        exigir(g.procesosEnMemoria() == 25 && g.getPlanificador().getListaTrabajos().tamanio() == 5, "Capacidad 25 y excedentes");
        invariantes(g);
        g.ejecutarTodo();
        exigir(g.getReloj() == 12 && g.getPlanificador().getListaFinalizados().tamanio() == 30, "Renovar grupos");
        invariantes(g);

        // Un error no impide el tick de las otras CPU en el mismo intervalo.
        g = gestor(2);
        BCP error = g.cargarPrograma(programa("POP AX", "INT 20H"), "error.asm", 0);
        BCP sano = g.cargarPrograma(programa("MOV AX, 4", "INC AX", "INT 10H", "INT 20H"), "sano.asm", 0);
        g.ejecutarUnPaso();
        exigir(BCP.FINALIZADO_ERROR.equals(error.getEstado()) && sano.getTiempoEmpleado() == 1, "Aislar error");
        g.ejecutarTodo();
        exigir(sano.getAxGuardado() == 5 && sano.getTiempoFin() == 4, "Otra CPU continúa");
        exigir(g.getMonitor().stream().anyMatch(l -> l.startsWith("P2 >")), "Salida CPU 2 visible en el monitor");

        g = gestor(5);
        BCP futuro = g.cargarPrograma(programa("INT 20H"), "futuro.asm", 7);
        for (int i = 0; i < 7; i++) g.ejecutarUnPaso();
        exigir(g.getReloj() == 7 && futuro.getTiempoInicio() == 7 && futuro.getTiempoEmpleado() == 0, "Espera global de llegada");
        g.ejecutarTodo();
        exigir(futuro.getTiempoFin() == 8, "Llegada no ejecuta antes de tiempo");

        // La RAM/BCP puede limitar residentes por debajo de 5*N, sin perder trabajo.
        g = new GestorProcesos(128, 32, 1024, 0, 5);
        for (int i = 0; i < 12; i++) g.cargarPrograma(programa("INC AX", "INT 20H"), "corto.asm", 0);
        g.ejecutarUnPaso();
        exigir(g.procesosEnMemoria() == 2 && g.getCpusOcupadas() == 2, "Respetar BCP disponibles");
        g.ejecutarTodo();
        exigir(!g.hayProcesos() && g.getReloj() == 12, "Drenar con memoria limitada");

        // Expropiación en varios núcleos: nunca ejecutar un proceso dos veces en un segundo.
        for (int n = 1; n <= 5; n++) for (int semilla = 0; semilla < 20; semilla++) {
            g = gestor(n);
            g.getPlanificador().setAlgoritmo(new AlgoritmoPlanificacion() {
                public String getNombre(){ return "RR prueba"; }
                public BCP seleccionar(ColaTrabajo c){ return c.desencolar(); }
                public boolean debeExpropiar(BCP b, int segundos, ColaTrabajo c){ return segundos >= 2 && !c.estaVacia(); }
            });
            Random azar = new Random(semilla);
            for (int i = 0; i < 28; i++) g.cargarPrograma(programa("MOV AX, " + i, "INC AX", "INC AX", "INT 20H"), "p" + i + ".asm", azar.nextInt(10));
            int limite = 0;
            while (g.hayProcesos()) {
                int[] antes = g.getTodosLosProcesos().stream().mapToInt(BCP::getTiempoEmpleado).toArray();
                int reloj = g.getReloj();
                exigir(g.ejecutarUnPaso() && ++limite < 500, "Avanzar hasta terminar");
                exigir(g.getReloj() == reloj + 1, "Un segundo global por paso");
                for (int i = 0; i < antes.length; i++) {
                    int delta = g.getTodosLosProcesos().get(i).getTiempoEmpleado() - antes[i];
                    exigir(delta == 0 || delta == 1, "Doble tick al migrar CPU");
                }
                invariantes(g);
            }
            for (BCP b : g.getTodosLosProcesos()) {
                exigir(BCP.FINALIZADO.equals(b.getEstado()) && b.getTiempoEmpleado() == 4, "Trabajo completo tras migraciones");
                exigir(b.getAxGuardado() == b.getIdProceso() + 1, "Registros restaurados al migrar");
            }
            escenarios++;
        }
        ConfiguracionSistema c = new ConfiguracionSistema();
        c.setCantidadCpus(5);
        Path archivo = Files.createTempFile("config-cpu", ".txt");
        try {
            c.guardarEnArchivo(archivo.toFile());
            ConfiguracionSistema copia = new ConfiguracionSistema();
            copia.cargarDesdeArchivo(archivo.toFile());
            exigir(copia.getCantidadCpus() == 5 && new GestorProcesos(copia).getCantidadCpus() == 5, "Persistir CPU");
        } finally { Files.deleteIfExists(archivo); }
        System.out.println("OK: " + escenarios + " escenarios de 1-5 CPU, reloj, 25 residentes, errores, llegadas, memoria y expropiación");
    }
}
