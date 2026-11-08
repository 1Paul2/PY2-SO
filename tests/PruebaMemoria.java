import java.nio.file.Files;
import java.util.*;
import minipc.hardware.*;
import minipc.modelo.*;
import minipc.util.*;

public class PruebaMemoria {
    static void verificar(boolean condicion, String mensaje){
        if (!condicion) throw new AssertionError(mensaje);
    }
    static ConfiguracionSistema configuracion(EstrategiaMemoria estrategia, int cpus){
        ConfiguracionSistema c = new ConfiguracionSistema();
        c.setCantidadCpus(cpus);
        c.setQuantumRR(1);
        c.setConfiguracionMemoria(256, 80, 2048, 128, estrategia, 4, 16, "12,16,24,32");
        return c;
    }
    static BCP reserva(GestorMemoria gestor, int longitud, int id){
        BCP proceso = new BCP(id, "Nuevo");
        proceso.setTamanioProceso(longitud);
        verificar(gestor.cargar(proceso, Collections.nCopies(longitud, "INC AX")), "Reserva inicial");
        return proceso;
    }
    static void dinamicas(){
        for (EstrategiaMemoria estrategia : List.of(EstrategiaMemoria.FIRST_FIT, EstrategiaMemoria.NEXT_FIT, EstrategiaMemoria.BEST_FIT)) {
            ConfiguracionSistema c = configuracion(estrategia, 1);
            c.setConfiguracionMemoria(128, 16, 512, 0, estrategia, 4, 16, "16,32");
            GestorMemoria gestor = new GestorMemoria(new Memoria(128, 16), new Disco(512, 0), c);
            BCP a = reserva(gestor, 8, 1);
            reserva(gestor, 4, 2);
            BCP b = reserva(gestor, 6, 3);
            reserva(gestor, 4, 4);
            gestor.liberar(a);
            gestor.liberar(b);
            BCP siguiente = reserva(gestor, 5, 5);
            int esperado = switch (estrategia) { case FIRST_FIT -> 16; case BEST_FIT -> 28; default -> 38; };
            verificar(siguiente.getDireccionBase() == esperado, "Hueco de " + estrategia);
        }
        ConfiguracionSistema c = configuracion(EstrategiaMemoria.NEXT_FIT, 1);
        GestorMemoria gestor = new GestorMemoria(new Memoria(256, 80), new Disco(2048, 128), c);
        BCP a = reserva(gestor, 100, 1);
        reserva(gestor, 76, 2);
        gestor.liberar(a);
        verificar(reserva(gestor, 100, 3).getDireccionBase() == 80, "Next Fit envuelve el cursor");
    }
    static void fijas(){
        for (EstrategiaMemoria estrategia : List.of(EstrategiaMemoria.FIJA_IGUAL, EstrategiaMemoria.FIJA_VARIABLE)) {
            ConfiguracionSistema c = configuracion(estrategia, 1);
            Memoria ram = new Memoria(256, 80);
            GestorMemoria gestor = new GestorMemoria(ram, new Disco(2048, 128), c);
            BCP a = reserva(gestor, 3, 1);
            int reservado = estrategia == EstrategiaMemoria.FIJA_IGUAL ? 16 : 12;
            verificar(gestor.getReservadoRam() == reservado && ram.estaOcupada(80 + reservado - 1), "Reservar fragmentación");
            gestor.liberar(a);
            verificar(gestor.getReservadoRam() == 0 && !ram.estaOcupada(80 + reservado - 1), "Liberar partición completa");
            verificar(reserva(gestor, 3, 2).getDireccionBase() == 80, "Reutilizar partición");
        }
    }
    static void paginas(){
        ConfiguracionSistema c = configuracion(EstrategiaMemoria.PAGINACION_FISICA, 1);
        c.setConfiguracionMemoria(128, 16, 512, 0, EstrategiaMemoria.PAGINACION_FISICA, 8, 16, "16,32");
        GestorMemoria gestor = new GestorMemoria(new Memoria(128, 16), new Disco(512, 0), c);
        BCP a = reserva(gestor, 16, 1);
        reserva(gestor, 8, 2);
        reserva(gestor, 16, 3);
        gestor.liberar(a);
        BCP noContiguo = reserva(gestor, 24, 4);
        List<Integer> bases = gestor.getPaginas().stream().filter(p -> p.proceso() == noContiguo).map(GestorMemoria.Pagina::baseRam).toList();
        verificar(bases.equals(List.of(16, 24, 56)), "Marcos físicos no contiguos");
        verificar(gestor.leerInstruccion(noContiguo, 23).equals("INC AX"), "Traducir última página");

        c.setConfiguracionMemoria(128, 112, 512, 64, EstrategiaMemoria.PAGINACION_VIRTUAL, 4, 16, "16");
        Memoria ram = new Memoria(128, 112);
        Disco disco = new Disco(512, 64);
        gestor = new GestorMemoria(ram, disco, c);
        BCP proceso = new BCP(1, "Nuevo");
        proceso.setTamanioProceso(24);
        List<String> contenido = new ArrayList<>();
        for (int i = 0; i < 24; i++) contenido.add("MOV AX, " + i);
        verificar(gestor.cargar(proceso, contenido), "Programa mayor que RAM");
        verificar(gestor.direccionFisica(proceso, 16) == -1, "Página inicial en disco");
        gestor.leerInstruccion(proceso, 16);
        verificar(gestor.direccionFisica(proceso, 0) == -1, "FIFO expulsa primera página");
        for (int i = 23; i >= 0; i--) verificar(gestor.leerInstruccion(proceso, i).equals(contenido.get(i)), "Conservar datos al intercambiar");
        verificar(gestor.getFallosPagina() > 1, "Registrar fallos");
        int uso = gestor.getReservadoRam();
        BCP imposible = new BCP(2, "Nuevo");
        imposible.setTamanioProceso(68);
        verificar(!gestor.cargar(imposible, Collections.nCopies(68, "INC AX")), "Fallo sin reservas parciales");
        verificar(gestor.getReservadoRam() == uso, "No filtrar marcos al fallar");
        gestor.liberar(proceso);
        verificar(gestor.getPaginas().isEmpty() && gestor.getReservadoRam() == 0 && disco.virtualUsada() == 0, "Liberar RAM y disco");
    }
    static List<Instruccion> programa(){
        String[] lineas = {"MOV BX, 3", "MOV AX, 0", "INC AX", "CMP AX, BX", "JNE -2", "MOV CX, 7", "PUSH AX", "POP DX", "INT 10H", "INT 20H"};
        return Arrays.stream(lineas).map(ParserASM::procesarLinea).toList();
    }
    static void ejecucion(){
        ConfiguracionSistema errorConfig = configuracion(EstrategiaMemoria.PAGINACION_VIRTUAL, 1);
        errorConfig.setConfiguracionMemoria(128, 112, 2048, 128, EstrategiaMemoria.PAGINACION_VIRTUAL, 4, 16, "16");
        GestorProcesos errorGestor = new GestorProcesos(errorConfig);
        List<Instruccion> invalido = new ArrayList<>();
        invalido.add(ParserASM.procesarLinea("JMP -1"));
        for (int i = 1; i < 20; i++) invalido.add(ParserASM.procesarLinea("INC AX"));
        BCP error = errorGestor.cargarPrograma(invalido, "error.asm", 0);
        errorGestor.ejecutarTodo();
        verificar(BCP.FINALIZADO_ERROR.equals(error.getEstado()) && errorGestor.getGestorMemoria().getReservadoRam() == 0
            && errorGestor.getDisco().virtualUsada() == 0, "Liberar memoria al finalizar con error");
        int escenarios = 0;
        for (EstrategiaMemoria estrategia : EstrategiaMemoria.values()) {
            for (String algoritmo : List.of("FCFS", "SRT", "SPN", "RR", "HRRN", "SRR", "Lottery", "Prioridad", "Guaranteed", "MLFQ")) {
                for (int cpus = 1; cpus <= 5; cpus++) {
                    ConfiguracionSistema c = configuracion(estrategia, cpus);
                    c.setAlgoritmoCpu(algoritmo);
                    if (estrategia == EstrategiaMemoria.PAGINACION_VIRTUAL)
                        c.setConfiguracionMemoria(128, 112, 2048, 128, estrategia, 4, 16, "16");
                    GestorProcesos g = new GestorProcesos(c);
                    List<BCP> procesos = new ArrayList<>();
                    for (int i = 0; i < 8; i++) procesos.add(g.cargarPrograma(programa(), "bucle.asm", i % 3));
                    int pasos = 0;
                    while (g.hayProcesos()) {
                        verificar(g.ejecutarUnPaso() && ++pasos < 2000, "Progreso con " + estrategia + " / " + algoritmo);
                        Set<Integer> ram = new HashSet<>();
                        for (GestorMemoria.Bloque bloque : g.getGestorMemoria().getBloquesFisicos()) {
                            verificar(bloque.base() >= g.getMemoria().getInicioUsuario(), "Proteger S.O.");
                            for (int i = 0; i < bloque.tamanio(); i++) verificar(ram.add(bloque.base() + i), "Evitar solapamiento");
                        }
                        Set<Integer> respaldo = new HashSet<>();
                        for (GestorMemoria.Pagina pagina : g.getGestorMemoria().getPaginas()) {
                            verificar((pagina.baseRam() >= 0) != (pagina.baseDisco() >= 0), "Página en una única ubicación");
                            if (pagina.baseDisco() >= 0) verificar(respaldo.add(pagina.baseDisco()), "Disco sin solapamientos");
                        }
                    }
                    for (BCP b : procesos) verificar(BCP.FINALIZADO.equals(b.getEstado()) && b.getAxGuardado() == 3
                        && b.getDxGuardado() == 3 && b.getCxGuardado() == 7 && b.getTiempoEmpleado() == 16,
                        "Ejecución de saltos y contexto con " + estrategia + " / " + algoritmo);
                    verificar(g.getGestorMemoria().getReservadoRam() == 0 && g.getDisco().virtualUsada() == 0, "Liberación final");
                    if (estrategia == EstrategiaMemoria.PAGINACION_VIRTUAL) verificar(g.getGestorMemoria().getFallosPagina() > 0, "Usar memoria virtual");
                    escenarios++;
                }
            }
        }
        System.out.println("OK: " + escenarios + " combinaciones de estrategia, algoritmo y cantidad de CPU");
    }
    static void configuracion() throws Exception {
        ConfiguracionSistema c = configuracion(EstrategiaMemoria.FIJA_VARIABLE, 1);
        var archivo = Files.createTempFile("memoria", ".txt");
        try {
            c.guardarEnArchivo(archivo.toFile());
            ConfiguracionSistema copia = new ConfiguracionSistema();
            copia.cargarDesdeArchivo(archivo.toFile());
            verificar(Arrays.equals(c.getParticiones(), copia.getParticiones()) && copia.getEstrategiaMemoria() == EstrategiaMemoria.FIJA_VARIABLE, "Persistencia");
            Files.writeString(archivo, "memoriaPrincipal=256\nmemoriaSistema=80\n");
            copia.cargarDesdeArchivo(archivo.toFile());
            verificar(copia.getEstrategiaMemoria() == EstrategiaMemoria.FIRST_FIT, "Archivos anteriores");
            try {
                copia.setConfiguracionMemoria(128, 112, 512, 0, EstrategiaMemoria.PAGINACION_FISICA, 32, 16, "16");
                throw new AssertionError("Aceptar configuración inválida");
            } catch (IllegalArgumentException esperado) { }
            verificar(copia.getMemoriaPrincipal() == 256, "Validación atómica");
        } finally { Files.deleteIfExists(archivo); }
    }
    public static void main(String[] args) throws Exception {
        dinamicas(); fijas(); paginas(); configuracion(); ejecucion();
        System.out.println("OK: huecos, particiones, páginas no contiguas, FIFO, validación y compatibilidad");
    }
}
