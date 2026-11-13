package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JPanel;
import minipc.gui.Componentes.BarraUso;
import minipc.gui.Componentes.Tarjeta;
import minipc.gui.Tema;
import minipc.hardware.Disco;
import minipc.hardware.Memoria;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;
import minipc.modelo.Planificador;
import minipc.util.ConfiguracionSistema;

/**
 * PanelRecursos: tarjeta "Recursos del sistema" con las barras de uso de procesos en
 * memoria, memoria de usuario, memoria virtual, zona del S.O. (BCP) y almacenamiento.
 */
public class PanelRecursos extends Tarjeta {

    private final ConfiguracionSistema configuracion;
    private final BarraUso barraProcesos, barraMemoria, barraBCP, barraDisco, barraVirtual;

    // E: configuracion (ConfiguracionSistema) - se usa para los totales cuando no hay programas
    // S: no aplica (constructor)
    // R: ninguna
    public PanelRecursos(ConfiguracionSistema configuracion){
        super("Recursos del sistema", Tema.AZUL);
        this.configuracion = configuracion;
        JPanel barras = new JPanel(new GridLayout(5, 1, 0, 10));
        barras.setOpaque(false);
        barraProcesos = new BarraUso("Procesos en memoria", Tema.VERDE);
        barraMemoria = new BarraUso("Memoria de usuario", Tema.AZUL);
        barraBCP = new BarraUso("Zona S.O. (BCP)", Tema.NARANJA_SO);
        barraDisco = new BarraUso("Almacenamiento", Tema.MORADO);
        barraVirtual = new BarraUso("Memoria virtual", new Color(0x14B8A6));
        barras.add(barraProcesos);
        barras.add(barraMemoria);
        barras.add(barraVirtual);
        barras.add(barraBCP);
        barras.add(barraDisco);
        getContenido().add(barras, BorderLayout.NORTH);
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: ninguna
    // Correccion - Poll (observación PY1): la capacidad de la zona del S.O. se cuenta en bloques de
    // BCP (16 posiciones cada uno), no en posiciones sueltas
    public void actualizar(GestorProcesos gestor){
        if (gestor == null) {
            int usuario = configuracion.getMemoriaPrincipal() - configuracion.getMemoriaSistema();
            barraProcesos.setValor(0, Planificador.MAX_PROCESOS_EN_MEMORIA * configuracion.getCantidadCpus());
            barraMemoria.setValor(0, usuario);
            barraBCP.setValor(0, Math.min(Planificador.MAX_PROCESOS_EN_MEMORIA * configuracion.getCantidadCpus(),
                configuracion.getMemoriaSistema() / BCP.TAMANO_BLOQUE));
            barraDisco.setValor(0, configuracion.getAlmacenamientoSecundario());
            barraVirtual.setValor(0, configuracion.getMemoriaVirtual());
            return;
        }
        Memoria m = gestor.getMemoria();
        Disco d = gestor.getDisco();
        int usuarioTotal = m.getTamanoTotal() - m.getInicioUsuario();
        int bcps = 0;
        for (BCP b : gestor.getTodosLosProcesos()) {
            String e = b.getEstado();
            if (!"Nuevo".equals(e) && !b.estaFinalizado()) {
                // La reserva física puede incluir fragmentación o solo algunas páginas.
                if (b.getDireccionBCP() >= 0) bcps++;
            }
        }
        int usuarioUsado = gestor.getGestorMemoria().getReservadoRam();
        int discoUsado = 0;
        for (int i = 0; i < d.getTamanoTotal(); i++) {
            String v = d.leer(i);
            if (v != null && !v.isEmpty()) discoUsado++;
        }
        barraProcesos.setValor(gestor.procesosEnMemoria(), gestor.getPlanificador().getMaxProcesosEnMemoria());
        barraMemoria.setValor(usuarioUsado, usuarioTotal);
        barraBCP.setValor(bcps, gestor.getPlanificador().getCapacidadBCP());
        barraDisco.setValor(discoUsado, d.getTamanoTotal());
        barraVirtual.setValor(d.virtualUsada(), d.getTamanoMemoriaVirtual());
    }
}
