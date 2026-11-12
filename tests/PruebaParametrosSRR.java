import java.io.File;
import java.util.ArrayList;
import java.util.List;
import minipc.modelo.GestorProcesos;
import minipc.modelo.Instruccion;
import minipc.modelo.algoritmos.AlgoritmoPlanificacion;
import minipc.modelo.algoritmos.FCFS;
import minipc.modelo.algoritmos.RoundRobin;
import minipc.modelo.algoritmos.SelfishRoundRobin;
import minipc.util.ConfiguracionSistema;
import minipc.util.ParserASM;

/**
 * Verifica que las tasas a y b de Selfish RR cambian el comportamiento como dice la teoría:
 * con b = 0 es igual a RR, con b >= a es igual a FCFS, y que se guardan en config.txt.
 */
public class PruebaParametrosSRR {

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    // Ejecuta los mismos 5 programas con el algoritmo dado y devuelve la línea de tiempo
    // ("P1 P1 P2 ..." por CPU), igual que la tabla de ejecución.
    private static String lineaDeTiempo(AlgoritmoPlanificacion algoritmo, int cpus) {
        GestorProcesos g = new GestorProcesos(256, 160, 512, 64, cpus);
        g.getPlanificador().setAlgoritmo(algoritmo);
        int[][] procesos = {{0, 5}, {1, 3}, {2, 6}, {4, 2}, {6, 4}};   // {llegada, ráfaga}
        for (int i = 0; i < procesos.length; i++) {
            List<Instruccion> programa = new ArrayList<>();
            for (int k = 1; k < procesos[i][1]; k++) programa.add(ParserASM.procesarLinea("INC AX"));
            programa.add(ParserASM.procesarLinea("INT 20H"));
            g.cargarPrograma(programa, "p" + (i + 1) + ".asm", procesos[i][0]);
        }
        g.ejecutarTodo();
        StringBuilder sb = new StringBuilder();
        for (int cpu = 0; cpu < cpus; cpu++) {
            sb.append("CPU").append(cpu + 1).append(':');
            for (int[] segundo : g.getHistorial()) sb.append(' ').append(segundo[cpu] == 0 ? "-" : "P" + segundo[cpu]);
            sb.append('\n');
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        for (int cpus = 1; cpus <= 2; cpus++) {
            for (int q = 1; q <= 3; q++) {
                String rr = lineaDeTiempo(new RoundRobin(q), cpus);
                String fcfs = lineaDeTiempo(new FCFS(), cpus);
                String porDefecto = lineaDeTiempo(new SelfishRoundRobin(q), cpus);
                String explicito = lineaDeTiempo(new SelfishRoundRobin(q, 2, 1), cpus);
                verificar(porDefecto.equals(explicito), "a=2, b=1 debe ser lo mismo que el SRR original");
                verificar(lineaDeTiempo(new SelfishRoundRobin(q, 2, 0), cpus).equals(rr), "b=0 debe comportarse como RR (q=" + q + ", " + cpus + " CPU)");
                if (cpus == 1) {
                    verificar(lineaDeTiempo(new SelfishRoundRobin(q, 1, 1), cpus).equals(fcfs), "b=a debe comportarse como FCFS (q=" + q + ")");
                    verificar(lineaDeTiempo(new SelfishRoundRobin(q, 2, 5), cpus).equals(fcfs), "b>a debe comportarse como FCFS (q=" + q + ")");
                }
                if (cpus == 1 && q == 2) {
                    System.out.println("RR q=2:\n" + rr + "SRR a=2 b=1:\n" + porDefecto + "SRR a=2 b=0:\n"
                        + lineaDeTiempo(new SelfishRoundRobin(q, 2, 0), cpus) + "SRR a=1 b=1:\n"
                        + lineaDeTiempo(new SelfishRoundRobin(q, 1, 1), cpus) + "FCFS:\n" + fcfs);
                }
            }
        }

        // Validación de las tasas
        for (int[] malas : new int[][]{{0, 1}, {-1, 0}, {2, -1}}) {
            try {
                new SelfishRoundRobin(2, malas[0], malas[1]);
                throw new AssertionError("Debió rechazar a=" + malas[0] + ", b=" + malas[1]);
            } catch (IllegalArgumentException esperado) { }
        }

        // Se guardan y se leen de config.txt, y el gestor las usa
        File archivo = File.createTempFile("config-srr", ".txt");
        archivo.deleteOnExit();
        ConfiguracionSistema c = new ConfiguracionSistema();
        c.setAlgoritmoCpu("SRR");
        c.setQuantumRR(2);
        c.setParametrosSRR(3, 0);
        c.guardarEnArchivo(archivo);
        ConfiguracionSistema leida = new ConfiguracionSistema();
        leida.cargarDesdeArchivo(archivo);
        verificar(leida.getTasaNuevosSRR() == 3 && leida.getTasaAceptadosSRR() == 0, "Las tasas se deben guardar y leer");
        GestorProcesos g = new GestorProcesos(leida);
        verificar("SRR".equals(g.getPlanificador().getAlgoritmo().getNombre()), "El gestor debe usar SRR");

        System.out.println("OK: tasas de Selfish RR (por defecto, b=0 como RR, b>=a como FCFS, validación y config.txt)");
    }
}
