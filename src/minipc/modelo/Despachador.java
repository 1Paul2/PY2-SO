package minipc.modelo;
import minipc.hardware.CPU;

public class Despachador {
    private CPU cpu;
    public Despachador(CPU cpu) { this.cpu = cpu; }
    // Correccion - Poll: ya no reinicia el peso de la instrucción (cada instrucción dura 1 s)
    public void despachar(BCP bcp) {
        cpu.setBcpActual(bcp);
        bcp.restaurarEstado(cpu);
        bcp.setEstado("Ejecución");
    }
    public void capturar(BCP bcp) { bcp.capturaEstado(cpu); }
}