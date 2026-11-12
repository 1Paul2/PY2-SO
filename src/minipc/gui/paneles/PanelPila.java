package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import javax.swing.JComponent;
import minipc.gui.Tema;
import minipc.modelo.BCP;

/** Dibuja las 5 posiciones de la pila del proceso actual con la flecha del tope (SP). */
public class PanelPila extends JComponent {
    private BCP bcp;

    public PanelPila(){ setPreferredSize(new Dimension(200, 140)); setMinimumSize(new Dimension(200, 120)); }

    public void setBCP(BCP b){ this.bcp = b; repaint(); }

    @Override
    protected void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int filas = 5;
        int gap = 4;
        int alto = Math.max(16, (getHeight() - gap * (filas - 1)) / filas);
        int xCaja = 60, wCaja = getWidth() - xCaja - 10;
        int tope = bcp == null ? -1 : bcp.getTopePila();
        String[] valores = new String[filas];
        if (bcp != null) {
            String p = bcp.pilaTexto();          // "[1,2,3]"
            String interior = p.substring(1, p.length() - 1);
            String[] partes = interior.isEmpty() ? new String[0] : interior.split(",");
            for (int i = 0; i < partes.length && i < filas; i++) valores[i] = partes[i];
        }
        for (int i = filas - 1, fila = 0; i >= 0; i--, fila++) {
            int y = fila * (alto + gap);
            boolean llena = i <= tope;
            g2.setColor(llena ? Tema.AMBAR_CLARO : new Color(0xF8FAF9));
            g2.fillRoundRect(xCaja, y, wCaja, alto, 10, 10);
            g2.setColor(llena ? Tema.AMBAR : Tema.BORDE);
            g2.drawRoundRect(xCaja, y, wCaja - 1, alto - 1, 10, 10);

            g2.setFont(Tema.PEQUENA);
            g2.setColor(Tema.TEXTO_SUAVE);
            g2.drawString(String.valueOf(i), xCaja - 14, y + alto / 2 + 4);

            g2.setFont(Tema.MONO_NORMAL);
            g2.setColor(llena ? Tema.TEXTO : new Color(0xB6C2BB));
            String texto = llena ? valores[i] : "vacío";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(texto, xCaja + (wCaja - fm.stringWidth(texto)) / 2, y + (alto + fm.getAscent()) / 2 - 2);

            if (i == tope) {
                g2.setFont(Tema.PEQUENA_B);
                g2.setColor(Tema.AMBAR);
                g2.drawString("SP", 2, y + alto / 2 + 4);
                int ax = 22, ay = y + alto / 2;
                g2.fillPolygon(new int[]{ax + 18, ax + 10, ax + 10}, new int[]{ay, ay - 5, ay + 5}, 3);
                g2.drawLine(ax, ay, ax + 10, ay);
            }
        }
        if (tope < 0) {
            g2.setFont(Tema.PEQUENA_B);
            g2.setColor(Tema.TEXTO_SUAVE);
            g2.drawString("SP", 2, (filas - 1) * (alto + gap) + alto + 0);
        }
        g2.dispose();
    }
}
