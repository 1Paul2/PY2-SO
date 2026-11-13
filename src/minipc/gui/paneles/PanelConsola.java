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
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.StyledDocument;
import minipc.gui.Componentes;
import minipc.gui.Tema;
import minipc.modelo.GestorProcesos;

/**
 * PanelConsola: pestaña "Consola del S.O.". Muestra lo que hace el sistema operativo:
 * despachador, cambios de contexto, cargas, archivos (INT 21H) y errores en rojo.
 */
public class PanelConsola extends JPanel {

    private final JTextPane area = new JTextPane();

    // E: no aplica
    // S: no aplica (constructor)
    // R: ninguna
    public PanelConsola(){
        super(new BorderLayout());
        setOpaque(false);
        area.setEditable(false);
        area.setFont(Tema.MONO_NORMAL);
        area.setBackground(new Color(0xFBFDFB));
        area.setBorder(new EmptyBorder(10, 12, 10, 12));
        add(Componentes.scroll(area), BorderLayout.CENTER);
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: ninguna
    public void actualizar(GestorProcesos gestor){
        StyledDocument doc = area.getStyledDocument();
        area.setText("");
        if (gestor != null) {
            for (String linea : gestor.getConsola()) {
                if (linea.startsWith("ERROR")) Componentes.agregarTexto(doc, linea + "\n", Tema.ROJO, true, false);
                else if (linea.startsWith(">>")) Componentes.agregarTexto(doc, linea + "\n", Tema.AMBAR, true, false);
                else if (linea.startsWith("[SO]")) Componentes.agregarTexto(doc, linea + "\n", Tema.TEXTO_SUAVE, false, true);
                else if (linea.startsWith("P")) Componentes.agregarTexto(doc, linea + "\n", Tema.VERDE_OSCURO, true, false);
                else Componentes.agregarTexto(doc, linea + "\n", Tema.AZUL, false, false);
            }
        }
        area.setCaretPosition(doc.getLength());
    }
}
