package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import minipc.gui.Tema;
import minipc.hardware.CPU;
import minipc.hardware.GestorMemoria;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;

/**
 * MapaMemoria: dibuja la memoria principal como una barra vertical dividida
 * en zonas: S.O. (con los BCP), un bloque de color por proceso cargado y el
 * espacio libre. Marca con una flecha la posición del PC.
 */
public class MapaMemoria extends JComponent {

    private GestorProcesos gestor;

    public MapaMemoria(){
        setPreferredSize(new Dimension(260, 300));
        setToolTipText("");
    }

    public void setGestor(GestorProcesos gestor){
        this.gestor = gestor;
        repaint();
    }

    // Correccion - Poll (observación PY1): "Finalizado con error" tampoco tiene memoria asignada
    // Procesos que tienen memoria asignada en este momento
    private List<BCP> procesosEnMemoria(){
        List<BCP> lista = new ArrayList<>();
        if (gestor == null) return lista;
        for (BCP b : gestor.getTodosLosProcesos()) {
            String e = b.getEstado();
            if (!"Nuevo".equals(e) && !b.estaFinalizado() && b.getDireccionBase() >= 0) {
                lista.add(b);
            }
        }
        return lista;
    }

    // Correccion - Poll: cada bloque se dibuja con el alcance completo; se quitan la etiqueta
    // "(+N virtual)" y el aviso "PC en memoria virtual" (memoria virtual partida)
    @Override
    protected void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int margenIzq = 44, margenDer = 18, arriba = 4, abajo = 4;
        int x = margenIzq;
        int w = getWidth() - margenIzq - margenDer;
        int h = getHeight() - arriba - abajo;

        if (gestor == null) {
            g2.setColor(Tema.GRIS_CLARO);
            g2.fillRoundRect(x, arriba, w, h, 12, 12);
            g2.setColor(Tema.TEXTO_SUAVE);
            g2.setFont(Tema.NORMAL);
            dibujarCentrado(g2, "Cargá programas para ver la memoria", x, arriba, w, h);
            g2.dispose();
            return;
        }

        int total = gestor.getMemoria().getTamanoTotal();
        int inicioUsuario = gestor.getMemoria().getInicioUsuario();
        double escala = (double) h / total;

        // Fondo: espacio libre con rayas suaves
        g2.setColor(new Color(0xF8FAF9));
        g2.fillRoundRect(x, arriba, w, h, 12, 12);
        g2.setClip(x, arriba, w, h);
        g2.setColor(new Color(0xEAF0EC));
        for (int i = -h; i < w + h; i += 10) {
            g2.drawLine(x + i, arriba + h, x + i + h, arriba);
        }
        g2.setClip(null);

        // Zona del S.O.
        int ySO = arriba;
        int hSO = Math.max(14, (int) Math.round(inicioUsuario * escala));
        g2.setColor(Tema.aclarar(Tema.NARANJA_SO, 0.55f));
        g2.fillRoundRect(x, ySO, w, hSO, 10, 10);
        g2.setColor(Tema.NARANJA_SO.darker());
        g2.setFont(Tema.PEQUENA_B);
        int bcps = 0;
        for (BCP b : procesosEnMemoria()) if (b.getDireccionBCP() >= 0) bcps++;
        dibujarCentrado(g2, "S.O. · " + bcps + " BCP", x, ySO, w, hSO);
        etiquetaDireccion(g2, 0, ySO);

        for (GestorMemoria.Bloque bloque : gestor.getGestorMemoria().getBloquesFisicos()) {
            int y = arriba + (int) Math.round(bloque.base() * escala);
            int alto = Math.max(1, (int) Math.round(bloque.tamanio() * escala));
            BCP proceso = bloque.proceso();
            g2.setColor(Tema.colorProceso(proceso.getIdProceso()));
            g2.fillRect(x, y, w, alto);
            g2.setColor(Color.WHITE);
            g2.setFont(Tema.PEQUENA_B);
            dibujarCentrado(g2, "P" + proceso.getIdProceso() + " · " + bloque.base()
                + "–" + (bloque.base() + bloque.tamanio() - 1), x, y, w, alto);
        }
        for (CPU cpu : gestor.getCpus()) {
            BCP proceso = cpu.getBcpActual();
            if (proceso == null) continue;
            int pc = gestor.getGestorMemoria().direccionFisica(proceso, cpu.getPC() - proceso.getDireccionBase());
            if (pc < 0) continue;
            int yPC = arriba + (int) Math.round((pc + 0.5) * escala);
            int xf = x + w + 2;
            g2.setColor(Tema.ROJO);
            g2.fillPolygon(new int[]{xf, xf + 10, xf + 10}, new int[]{yPC, yPC - 4, yPC + 4}, 3);
        }

        etiquetaDireccion(g2, total - 1, arriba + h - 10);
        g2.dispose();
    }

    private void etiquetaDireccion(Graphics2D g2, int dir, int y){
        g2.setFont(Tema.PEQUENA);
        g2.setColor(Tema.TEXTO_SUAVE);
        String s = String.format("%04d", dir);
        g2.drawString(s, 4, y + 11);
    }

    private void dibujarCentrado(Graphics2D g2, String s, int x, int y, int w, int h){
        FontMetrics fm = g2.getFontMetrics();
        if (h < fm.getHeight() - 4) return;
        int tx = x + (w - fm.stringWidth(s)) / 2;
        int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(s, tx, ty);
    }

    @Override
    public String getToolTipText(java.awt.event.MouseEvent e){
        if (gestor == null) return null;
        int total = gestor.getMemoria().getTamanoTotal();
        int h = getHeight() - 8;
        int pos = (int) ((e.getY() - 4) / ((double) h / total));
        if (pos < 0 || pos >= total) return null;
        return "Posición " + pos + ": " + gestor.getMemoria().leer(pos);
    }
}
