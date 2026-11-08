import java.util.ArrayList;
import java.util.List;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;
import minipc.modelo.Instruccion;
import minipc.modelo.algoritmos.SRT;
import minipc.util.ParserASM;

public class PruebaSRT {
    private static GestorProcesos gestor(int cpus) {
        GestorProcesos gestor = new GestorProcesos(256, 160, 512, 64, cpus);
        gestor.getPlanificador().setAlgoritmo(new SRT());
        return gestor;
    }

    private static BCP cargar(GestorProcesos gestor, String nombre, int rafaga, int llegada) {
        List<Instruccion> programa = new ArrayList<>();
        for (int i = 1; i < rafaga; i++) programa.add(ParserASM.procesarLinea("INC AX"));
        programa.add(ParserASM.procesarLinea("INT 20H"));
        return gestor.cargarPrograma(programa, nombre + ".asm", llegada);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    public static void main(String[] args) {
        GestorProcesos g = gestor(2);
        BCP largo = cargar(g, "largo", 8, 0);
        BCP medio = cargar(g, "medio", 5, 0);
        BCP corto = cargar(g, "corto", 2, 1);
        g.ejecutarUnPaso();
        verificar(g.getCpu(0).getBcpActual() == medio, "Conservar la CPU con menor restante");
        verificar(g.getCpu(1).getBcpActual() == corto, "Desalojar solamente al mas largo");
        g.ejecutarTodo();
        verificar(corto.getTiempoFin() == 3 && largo.getTiempoFin() == 10, "Finales tras expropiar");
        verificar(largo.getAxGuardado() == 7, "Preservar registros al retomar");

        g = gestor(2);
        largo = cargar(g, "largo", 5, 0);
        corto = cargar(g, "corto", 1, 1);
        g.ejecutarUnPaso();
        verificar(g.getCpu(0).getBcpActual() == largo && g.getCpu(1).getBcpActual() == corto,
            "Usar la CPU libre antes de desalojar");

        g = gestor(1);
        largo = cargar(g, "actual", 4, 0);
        cargar(g, "empate", 3, 1);
        g.ejecutarUnPaso();
        verificar(g.getCpu().getBcpActual() == largo, "Mantener al proceso actual en un empate");
        System.out.println("OK: SRT, CPU libre, empate, expropiacion y contexto");
    }
}
