package minipc.hardware;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import minipc.modelo.BCP;
import minipc.util.ConfiguracionSistema;

/** Reserva, traducción de direcciones y liberación compartidas por todas las CPU. */
public class GestorMemoria {
    public record Bloque(BCP proceso, int base, int tamanio, int instrucciones){}
    public record Pagina(BCP proceso, int numero, int baseRam, int baseDisco){}
    private record Particion(int base, int tamanio){}
    private static class PaginaInterna {
        BCP proceso;
        int numero;
        int ram = -1;
        int disco = -1;
        long orden;
    }
    private static class Asignacion {
        int base;
        int reservado;
        List<PaginaInterna> paginas = new ArrayList<>();
    }

    private final Memoria memoria;
    private final Disco disco;
    private final EstrategiaMemoria estrategia;
    private final int tamanoPagina;
    private final List<Particion> particiones = new ArrayList<>();
    private final Map<BCP, Asignacion> asignaciones = new LinkedHashMap<>();
    private final boolean[] marcosRam;
    private final boolean[] marcosDisco;
    private int cursor;
    private long orden;
    private int fallosPagina;

    public GestorMemoria(Memoria memoria, Disco disco, ConfiguracionSistema config){
        this.memoria = memoria;
        this.disco = disco;
        estrategia = config == null ? EstrategiaMemoria.FIRST_FIT : config.getEstrategiaMemoria();
        tamanoPagina = config == null ? 8 : config.getTamanoPagina();
        cursor = memoria.getInicioUsuario();
        int usuario = memoria.getTamanoTotal() - cursor;
        marcosRam = new boolean[usuario / tamanoPagina];
        marcosDisco = new boolean[disco.getTamanoMemoriaVirtual() / tamanoPagina];
        if (estrategia.esPaginada() && marcosRam.length == 0)
            throw new IllegalArgumentException("Debe caber al menos un marco en la RAM de usuario");
        if (estrategia == EstrategiaMemoria.FIJA_IGUAL) {
            int tamano = config.getTamanoParticion();
            for (int base = cursor; base <= memoria.getTamanoTotal() - tamano; base += tamano)
                particiones.add(new Particion(base, tamano));
        } else if (estrategia == EstrategiaMemoria.FIJA_VARIABLE) {
            int base = cursor;
            for (int tamano : config.getParticiones()) {
                particiones.add(new Particion(base, tamano));
                base += tamano;
            }
        }
    }

    public EstrategiaMemoria getEstrategia(){ return estrategia; }
    public int getFallosPagina(){ return fallosPagina; }

    public int getMaximoPrograma(){
        if (estrategia.esPaginada()) {
            int marcos = marcosRam.length;
            if (estrategia == EstrategiaMemoria.PAGINACION_VIRTUAL) marcos += marcosDisco.length;
            return marcos * tamanoPagina;
        }
        if (!particiones.isEmpty()) return particiones.stream().mapToInt(Particion::tamanio).max().orElse(0);
        if (estrategia == EstrategiaMemoria.FIJA_IGUAL || estrategia == EstrategiaMemoria.FIJA_VARIABLE) return 0;
        return memoria.getTamanoTotal() - memoria.getInicioUsuario();
    }

    public boolean cargar(BCP proceso, List<String> instrucciones){
        if (asignaciones.containsKey(proceso)) throw new IllegalStateException("El proceso ya tiene memoria");
        if (instrucciones.isEmpty() || instrucciones.size() > getMaximoPrograma())
            throw new IllegalArgumentException("El programa no cabe con la estrategia seleccionada");
        Asignacion asignacion = new Asignacion();
        if (estrategia.esPaginada()) {
            if (!cargarPaginas(proceso, instrucciones, asignacion)) return false;
            // PC y saltos usan direcciones lógicas desde cero en paginación.
            asignacion.base = 0;
        } else {
            int reservado = instrucciones.size();
            int base = -1;
            if (!particiones.isEmpty()) {
                for (Particion particion : particiones) {
                    if (particion.tamanio() >= reservado && libre(particion.base(), particion.tamanio())) {
                        base = particion.base();
                        reservado = particion.tamanio();
                        break;
                    }
                }
            } else {
                base = buscarHueco(reservado);
            }
            if (base < 0) return false;
            for (int i = 0; i < reservado; i++)
                memoria.escribir(base + i, i < instrucciones.size() ? instrucciones.get(i) : "[Fragmentación interna]");
            asignacion.base = base;
            asignacion.reservado = reservado;
            cursor = base + reservado;
            if (cursor >= memoria.getTamanoTotal()) cursor = memoria.getInicioUsuario();
        }
        asignaciones.put(proceso, asignacion);
        proceso.setDireccionBase(asignacion.base);
        return true;
    }

    private boolean libre(int base, int tamano){
        for (int i = base; i < base + tamano; i++) if (memoria.estaOcupada(i)) return false;
        return true;
    }

    private int buscarHueco(int tamano){
        int inicio = memoria.getInicioUsuario();
        int fin = memoria.getTamanoTotal();
        if (estrategia == EstrategiaMemoria.NEXT_FIT) {
            // Recorrer direcciones candidatas desde el cursor y envolver una sola vez.
            for (int i = 0; i < fin - inicio; i++) {
                int base = inicio + (cursor - inicio + i) % (fin - inicio);
                if (base <= fin - tamano && libre(base, tamano)) return base;
            }
            return -1;
        }
        int mejor = -1;
        int menorHueco = Integer.MAX_VALUE;
        int base = inicio;
        while (base < fin) {
            if (memoria.estaOcupada(base)) { base++; continue; }
            int limite = base;
            while (limite < fin && !memoria.estaOcupada(limite)) limite++;
            int espacio = limite - base;
            if (espacio >= tamano) {
                if (estrategia != EstrategiaMemoria.BEST_FIT) return base;
                if (espacio < menorHueco) { mejor = base; menorHueco = espacio; }
            }
            base = limite;
        }
        return mejor;
    }

    private static int libres(boolean[] marcos){
        int total = 0;
        for (boolean ocupado : marcos) if (!ocupado) total++;
        return total;
    }

    private static int primerLibre(boolean[] marcos){
        for (int i = 0; i < marcos.length; i++) if (!marcos[i]) return i;
        return -1;
    }

    private boolean cargarPaginas(BCP proceso, List<String> instrucciones, Asignacion asignacion){
        int cantidad = (instrucciones.size() + tamanoPagina - 1) / tamanoPagina;
        int capacidad = libres(marcosRam);
        if (estrategia == EstrategiaMemoria.PAGINACION_VIRTUAL) capacidad += libres(marcosDisco);
        if (cantidad > capacidad) return false;
        for (int numero = 0; numero < cantidad; numero++) {
            PaginaInterna pagina = new PaginaInterna();
            pagina.proceso = proceso;
            pagina.numero = numero;
            List<String> contenido = new ArrayList<>();
            for (int i = 0; i < tamanoPagina; i++) {
                int indice = numero * tamanoPagina + i;
                contenido.add(indice < instrucciones.size() ? instrucciones.get(indice) : "[Fragmentación interna]");
            }
            pagina.ram = primerLibre(marcosRam);
            if (pagina.ram >= 0) {
                marcosRam[pagina.ram] = true;
                escribirMarco(pagina.ram, contenido);
                pagina.orden = orden++;
            } else {
                pagina.disco = primerLibre(marcosDisco);
                marcosDisco[pagina.disco] = true;
                disco.escribirVirtual(baseDisco(pagina.disco), contenido);
            }
            asignacion.paginas.add(pagina);
        }
        return true;
    }

    private int baseRam(int marco){ return memoria.getInicioUsuario() + marco * tamanoPagina; }
    private int baseDisco(int marco){ return disco.getInicioVirtual() + marco * tamanoPagina; }

    private void escribirMarco(int marco, List<String> contenido){
        for (int i = 0; i < tamanoPagina; i++) memoria.escribir(baseRam(marco) + i, contenido.get(i));
    }

    public String leerInstruccion(BCP proceso, int desplazamiento){
        if (desplazamiento < 0 || desplazamiento >= proceso.getTamanioProceso())
            throw new IllegalArgumentException("Acceso fuera del proceso");
        Asignacion asignacion = asignaciones.get(proceso);
        if (asignacion == null) throw new IllegalStateException("Proceso sin memoria asignada");
        if (!estrategia.esPaginada()) return memoria.leer(asignacion.base + desplazamiento);
        PaginaInterna pagina = asignacion.paginas.get(desplazamiento / tamanoPagina);
        if (pagina.ram < 0) traerPagina(pagina);
        return memoria.leer(baseRam(pagina.ram) + desplazamiento % tamanoPagina);
    }

    private void traerPagina(PaginaInterna pagina){
        List<String> contenido = new ArrayList<>();
        int origen = pagina.disco;
        for (int i = 0; i < tamanoPagina; i++) contenido.add(disco.leer(baseDisco(origen) + i));
        int marco = primerLibre(marcosRam);
        if (marco < 0) {
            PaginaInterna victima = asignaciones.values().stream().flatMap(a -> a.paginas.stream())
                .filter(p -> p.ram >= 0).min(Comparator.comparingLong(p -> p.orden)).orElseThrow();
            marco = victima.ram;
            List<String> respaldo = new ArrayList<>();
            for (int i = 0; i < tamanoPagina; i++) respaldo.add(memoria.leer(baseRam(marco) + i));
            disco.escribirVirtual(baseDisco(origen), respaldo);
            victima.disco = origen;
            victima.ram = -1;
        } else {
            marcosRam[marco] = true;
            marcosDisco[origen] = false;
            disco.liberarVirtual(baseDisco(origen), tamanoPagina);
        }
        escribirMarco(marco, contenido);
        pagina.ram = marco;
        pagina.disco = -1;
        pagina.orden = orden++;
        fallosPagina++;
    }

    /** Consulta para la GUI. No provoca fallos de página. */
    public int direccionFisica(BCP proceso, int desplazamiento){
        Asignacion asignacion = asignaciones.get(proceso);
        if (asignacion == null || desplazamiento < 0 || desplazamiento >= proceso.getTamanioProceso()) return -1;
        if (!estrategia.esPaginada()) return asignacion.base + desplazamiento;
        PaginaInterna pagina = asignacion.paginas.get(desplazamiento / tamanoPagina);
        return pagina.ram < 0 ? -1 : baseRam(pagina.ram) + desplazamiento % tamanoPagina;
    }

    public void liberar(BCP proceso){
        Asignacion asignacion = asignaciones.remove(proceso);
        if (asignacion == null) return;
        if (!estrategia.esPaginada()) memoria.liberar(asignacion.base, asignacion.reservado);
        for (PaginaInterna pagina : asignacion.paginas) {
            if (pagina.ram >= 0) {
                marcosRam[pagina.ram] = false;
                memoria.liberar(baseRam(pagina.ram), tamanoPagina);
            }
            if (pagina.disco >= 0) {
                marcosDisco[pagina.disco] = false;
                disco.liberarVirtual(baseDisco(pagina.disco), tamanoPagina);
            }
        }
    }

    public List<Bloque> getBloquesFisicos(){
        List<Bloque> bloques = new ArrayList<>();
        for (Map.Entry<BCP, Asignacion> entrada : asignaciones.entrySet()) {
            BCP proceso = entrada.getKey();
            Asignacion asignacion = entrada.getValue();
            if (!estrategia.esPaginada()) {
                bloques.add(new Bloque(proceso, asignacion.base, asignacion.reservado, proceso.getTamanioProceso()));
            }
            for (PaginaInterna pagina : asignacion.paginas) if (pagina.ram >= 0) {
                int usadas = Math.min(tamanoPagina, proceso.getTamanioProceso() - pagina.numero * tamanoPagina);
                bloques.add(new Bloque(proceso, baseRam(pagina.ram), tamanoPagina, usadas));
            }
        }
        bloques.sort(Comparator.comparingInt(Bloque::base));
        return bloques;
    }

    public List<Pagina> getPaginas(){
        List<Pagina> paginas = new ArrayList<>();
        for (Asignacion asignacion : asignaciones.values()) for (PaginaInterna pagina : asignacion.paginas)
            paginas.add(new Pagina(pagina.proceso, pagina.numero,
                pagina.ram < 0 ? -1 : baseRam(pagina.ram), pagina.disco < 0 ? -1 : baseDisco(pagina.disco)));
        return paginas;
    }

    public int getReservadoRam(){ return getBloquesFisicos().stream().mapToInt(Bloque::tamanio).sum(); }
}
