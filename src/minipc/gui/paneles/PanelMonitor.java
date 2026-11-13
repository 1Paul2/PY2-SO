package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.StyledDocument;
import minipc.gui.Componentes;
import minipc.gui.Tema;
import minipc.modelo.GestorProcesos;

/**
 * PanelMonitor: pestaña "Monitor". Muestra solo lo que imprimen los programas con INT 10H,
 * sobre fondo oscuro como una pantalla, con el "P#" del color de cada proceso.
 */
public class PanelMonitor extends JPanel {

    private static final Color FONDO = new Color(0x0F1A14);
    private final JTextPane area = new JTextPane();

    // E: no aplica
    // S: no aplica (constructor)
    // R: ninguna
    public PanelMonitor(){
        super(new BorderLayout());
        setOpaque(false);
        area.setEditable(false);
        area.setFont(Tema.MONO_NORMAL);
        area.setBackground(FONDO);
        area.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = Componentes.scroll(area);
        scroll.getViewport().setBackground(FONDO);
        add(scroll, BorderLayout.CENTER);
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: ninguna
    public void actualizar(GestorProcesos gestor){
        StyledDocument doc = area.getStyledDocument();
        area.setText("");
        if (gestor != null) {
            for (String linea : gestor.getMonitor()) {
                int corte = linea.indexOf(" > ");
                String quien = corte > 0 ? linea.substring(0, corte) : "";
                String texto = corte > 0 ? linea.substring(corte + 3) : linea;
                Color colorQuien = Tema.VERDE_CLARO;
                try { colorQuien = Tema.aclarar(Tema.colorProceso(Integer.parseInt(quien.substring(1))), 0.35f); }
                catch (RuntimeException sinId) { }
                Componentes.agregarTexto(doc, quien.isEmpty() ? "" : quien + "  ", colorQuien, true, false);
                Componentes.agregarTexto(doc, texto + "\n", new Color(0xE8F5EC), false, false);
            }
        }
        if (doc.getLength() == 0)
            Componentes.agregarTexto(doc, "(todavía ningún programa imprimió con INT 10H)", new Color(0x6B8A77), false, true);
        area.setCaretPosition(doc.getLength());
    }
}
