import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import minipc.hardware.CPU;
import minipc.modelo.*;
import minipc.modelo.algoritmos.*;
import minipc.util.*;

public class PruebaAlgoritmosRestantes {
    private static void verificar(boolean condicion, String mensaje){
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static GestorProcesos gestor(String algoritmo, int cpus, int boost){
        ConfiguracionSistema c = new ConfiguracionSistema();
        c.setTamanos(2048, 400, 4096, 0);
        c.setCantidadCpus(cpus);
        c.setAlgoritmoCpu(algoritmo);
        for (String a : new String[]{"RR", "SRR", "Lottery", "MLFQ"}) c.setQuantum(a, 2);
        c.setParametrosMLFQ(3, boost);
        return new GestorProcesos(c);
    }

    private static BCP cargar(GestorProcesos gestor, int longitud, int llegada){
        List<Instruccion> programa = new ArrayList<>();
        for (int i = 1; i < longitud; i++) programa.add(ParserASM.procesarLinea("INC AX"));
        programa.add(ParserASM.procesarLinea("INT 20H"));
        return gestor.cargarPrograma(programa, "programa" + longitud + ".asm", llegada);
    }

    private static void prioridad(){
        GestorProcesos g = gestor("Prioridad", 2, 20);
        BCP primero = cargar(g, 12, 0);
        BCP peor = cargar(g, 12, 0);
        primero.setPrioridad(3);
        peor.setPrioridad(9);
        BCP urgente = cargar(g, 2, 1);
        urgente.setPrioridad(1);
        g.ejecutarUnPaso();
        verificar(g.getCpu(0).getBcpActual() == primero, "Conservar prioridad intermedia");
        verificar(g.getCpu(1).getBcpActual() == urgente, "Desalojar la peor prioridad");
        g.ejecutarTodo();
        verificar(urgente.getTiempoFin() == 3 && peor.getAxGuardado() == 11, "Restaurar contexto");

        g = gestor("Prioridad", 1, 20);
        primero = cargar(g, 4, 0);
        primero.setPrioridad(2);
        BCP empate = cargar(g, 2, 1);
        empate.setPrioridad(2);
        g.ejecutarUnPaso();
        verificar(g.getCpu().getBcpActual() == primero, "No desalojar en empates");
    }

    private static void garantizado(){
        for (int cpus = 1; cpus <= 5; cpus++) {
            GestorProcesos g = gestor("Guaranteed", cpus, 20);
            List<BCP> procesos = new ArrayList<>();
            for (int i = 0; i < 12; i++) procesos.add(cargar(g, 40, 0));
            for (int t = 0; t < 20; t++) g.ejecutarUnPaso();
            List<BCP> residentes = procesos.stream().filter(p -> p.getDireccionBCP() >= 0).toList();
            int minimo = residentes.stream().mapToInt(BCP::getTiempoEmpleado).min().orElseThrow();
            int maximo = residentes.stream().mapToInt(BCP::getTiempoEmpleado).max().orElseThrow();
            verificar(maximo - minimo <= 1, "Reparto justo con " + cpus + " CPU");
            int antes = g.getTodosLosProcesos().size();
            BCP nuevo = cargar(g, 3, g.getReloj());
            nuevo.setPrioridad(1);
            g.ejecutarTodo();
            verificar(g.getPlanificador().getListaFinalizados().tamanio() == antes + 1, "Llegada tardía termina");
        }
    }

    private static void mlfq(){
        GestorProcesos g = gestor("MLFQ", 1, 100);
        BCP a = cargar(g, 20, 0);
        BCP b = cargar(g, 20, 0);
        for (int i = 0; i < 2; i++) g.ejecutarUnPaso();
        verificar(g.getCpu().getBcpActual() == b, "RR en nivel superior");
        for (int i = 0; i < 2; i++) g.ejecutarUnPaso();
        verificar(g.getCpu().getBcpActual() == a, "Descenso de ambos a nivel 1");
        for (int i = 0; i < 3; i++) g.ejecutarUnPaso();
        verificar(g.getCpu().getBcpActual() == a, "Quantum duplicado en nivel 1");
        g.ejecutarUnPaso();
        verificar(g.getCpu().getBcpActual() == b, "Vencimiento tras cuatro segundos de nivel 1");

        g = gestor("MLFQ", 1, 100);
        a = cargar(g, 20, 0);
        b = cargar(g, 20, 0);
        cargar(g, 1, 5);
        for (int i = 0; i < 13; i++) g.ejecutarUnPaso();
        verificar(a.getTiempoEmpleado() == 6 && g.getCpu().getBcpActual() == b,
            "El nivel conserva el consumo tras expropiacion por una llegada");

        g = gestor("MLFQ", 1, 5);
        a = cargar(g, 20, 0);
        b = cargar(g, 20, 0);
        for (int i = 0; i < 4; i++) g.ejecutarUnPaso();
        verificar(g.getCpu().getBcpActual() == a, "Preparar prueba de promoción");
        g.ejecutarUnPaso();
        verificar(g.getCpu().getBcpActual() == b, "Promoción global devuelve todos a nivel superior");

        g = gestor("MLFQ", 1, 100);
        a = cargar(g, 20, 0);
        BCP corto = cargar(g, 1, 3);
        for (int i = 0; i < 3; i++) g.ejecutarUnPaso();
        verificar(g.getCpu().getBcpActual() == corto, "Nuevo proceso expropia nivel inferior");
        g.ejecutarTodo();
        verificar(a.getTiempoEmpleado() == 20 && a.getAxGuardado() == 19, "Preservar contexto en MLFQ");
    }

    private static void regresionMulticpu(){
        for (String algoritmo : List.of("Prioridad", "Guaranteed", "MLFQ")) {
            for (int cpus = 1; cpus <= 5; cpus++) {
                GestorProcesos g = gestor(algoritmo, cpus, 7);
                List<BCP> procesos = new ArrayList<>();
                for (int i = 0; i < 28; i++) {
                    BCP proceso = cargar(g, 4 + i % 6, i % 8);
                    proceso.setPrioridad(1 + i % 5);
                    procesos.add(proceso);
                }
                int pasos = 0;
                while (g.hayProcesos()) {
                    int reloj = g.getReloj();
                    int[] anteriores = procesos.stream().mapToInt(BCP::getTiempoEmpleado).toArray();
                    verificar(g.ejecutarUnPaso() && ++pasos < 1000, "Avance de " + algoritmo);
                    verificar(g.getReloj() == reloj + 1, "Reloj global");
                    HashSet<BCP> vistos = new HashSet<>();
                    for (CPU cpu : g.getCpus()) {
                        if (cpu.getBcpActual() != null) verificar(vistos.add(cpu.getBcpActual()), "CPU duplicada");
                    }
                    for (int i = 0; i < procesos.size(); i++) {
                        int delta = procesos.get(i).getTiempoEmpleado() - anteriores[i];
                        verificar(delta == 0 || delta == 1, "Doble tick al migrar");
                    }
                }
                for (BCP proceso : procesos) {
                    verificar(BCP.FINALIZADO.equals(proceso.getEstado()), "Finalizar todos");
                    verificar(proceso.getTiempoEmpleado() == proceso.getRafaga(), "Consumo correcto");
                    verificar(proceso.getAxGuardado() == proceso.getRafaga() - 1, "Registros independientes");
                    verificar(proceso.getDireccionBCP() < 0, "Liberar memoria");
                }
            }
        }
    }

    private static void configuracion() throws Exception {
        ConfiguracionSistema c = new ConfiguracionSistema();
        c.setAlgoritmoCpu("MLFQ");
        c.setParametrosMLFQ(5, 37);
        c.setQuantum("MLFQ", Integer.MAX_VALUE);
        c.setQuantum("RR", 3);
        c.setQuantum("SRR", 4);
        c.setQuantum("Lottery", 5);
        var temporal = Files.createTempFile("mlfq", ".txt");
        try {
            c.guardarEnArchivo(temporal.toFile());
            ConfiguracionSistema copia = new ConfiguracionSistema();
            copia.cargarDesdeArchivo(temporal.toFile());
            // Cada algoritmo tiene su propio quantum: cambiar uno no cambia los demás
            verificar(copia.getQuantum("RR") == 3 && copia.getQuantum("SRR") == 4 && copia.getQuantum("Lottery") == 5
                && copia.getQuantum("MLFQ") == Integer.MAX_VALUE, "Quantum propio de cada algoritmo");
            verificar(ConfiguracionSistema.usaQuantum("RR") && !ConfiguracionSistema.usaQuantum("FCFS"), "Algoritmos con quantum");
            verificar(copia.getNivelesMLFQ() == 5 && copia.getPeriodoBoostMLFQ() == 37, "Persistir parámetros");
            verificar(new GestorProcesos(copia).getPlanificador().getAlgoritmo().getNombre().equals("MLFQ"), "Aplicar configuración");
            Files.writeString(temporal, "memoriaPrincipal=256\nmemoriaSistema=80\n");
            copia.cargarDesdeArchivo(temporal.toFile());
            verificar(copia.getAlgoritmoCpu().equals("FCFS") && copia.getNivelesMLFQ() == 3, "Configuración antigua");
            // Un config.txt de antes solo tenía quantumRR: los cuatro algoritmos toman ese valor
            Files.writeString(temporal, "memoriaPrincipal=256\nmemoriaSistema=80\nquantumRR=4\n");
            copia.cargarDesdeArchivo(temporal.toFile());
            verificar(copia.getQuantum("RR") == 4 && copia.getQuantum("SRR") == 4 && copia.getQuantum("Lottery") == 4
                && copia.getQuantum("MLFQ") == 4, "Quantum de un config.txt antiguo");
            try {
                c.setParametrosMLFQ(9, 0);
                throw new AssertionError("Parámetros inválidos aceptados");
            } catch (IllegalArgumentException esperado) { }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    public static void main(String[] args) throws Exception {
        prioridad();
        garantizado();
        mlfq();
        regresionMulticpu();
        configuracion();
        System.out.println("OK: Prioridad, Guaranteed y MLFQ, configuración, llegadas, 1-5 CPU y conservación de contexto");
    }
}
