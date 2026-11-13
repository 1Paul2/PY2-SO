package minipc.gui.paneles;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import minipc.gui.Componentes;
import minipc.gui.Componentes.Tarjeta;
import minipc.gui.Tema;
import minipc.modelo.GestorProcesos;

/**
 * PanelMonitorConsola: tarjeta "Monitor y consola" con dos pestañas. El monitor muestra
 * lo que imprimen los programas (INT 10H); la consola, lo que hace el sistema operativo
 * (despachador, memoria, archivos y errores).
 */
public class PanelMonitorConsola extends Tarjeta {

    private static final Color FONDO_MONITOR = new Color(0x0F1A14);
    private final JTextPane areaMonitor;    // salida de los programas (INT 10H)
    private final JTextPane areaConsola;    // mensajes del S.O.

    // E: no aplica
    // S: no aplica (constructor)
    // R: ninguna
    public PanelMonitorConsola(){
        super("Monitor y consola", new Color(0x0F766E));
        areaMonitor = new JTextPane();
        areaMonitor.setEditable(false);
        areaMonitor.setFont(Tema.MONO_NORMAL);
        areaMonitor.setBackground(FONDO_MONITOR);
        areaMonitor.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane scrollMonitor = Componentes.scroll(areaMonitor);
        scrollMonitor.getViewport().setBackground(FONDO_MONITOR);
        areaConsola = new JTextPane();
        areaConsola.setEditable(false);
        areaConsola.setFont(Tema.MONO_NORMAL);
        areaConsola.setBackground(new Color(0xFBFDFB));
        areaConsola.setBorder(new EmptyBorder(8, 10, 8, 10));
        JTabbedPane vistas = new JTabbedPane();
        vistas.addTab("Monitor", scrollMonitor);
        vistas.addTab("Consola del S.O.", Componentes.scroll(areaConsola));
        vistas.setToolTipTextAt(0, "Lo que imprimen los programas con INT 10H");
        vistas.setToolTipTextAt(1, "Lo que hace el sistema operativo: despachador, memoria, archivos y errores");
        getContenido().add(vistas, BorderLayout.CENTER);
    }

    // E: gestor (GestorProcesos) - puede ser null si todavía no hay programas
    // S: no aplica (void)
    // R: el monitor muestra solo la salida de los programas (INT 10H), con el "P#" del color del
    //    proceso; la consola muestra los mensajes del S.O. (errores en rojo, [SO] en gris)
    public void actualizar(GestorProcesos gestor){
        StyledDocument mon = areaMonitor.getStyledDocument();
        areaMonitor.setText("");
        if (gestor != null) {
            for (String linea : gestor.getMonitor()) {
                int corte = linea.indexOf(" > ");
                String quien = corte > 0 ? linea.substring(0, corte) : "";
                String texto = corte > 0 ? linea.substring(corte + 3) : linea;
                Color colorQuien = Tema.VERDE_CLARO;
                try { colorQuien = Tema.aclarar(Tema.colorProceso(Integer.parseInt(quien.substring(1))), 0.35f); }
                catch (RuntimeException sinId) { }
                agregar(mon, quien.isEmpty() ? "" : quien + "  ", colorQuien, true, false);
                agregar(mon, texto + "\n", new Color(0xE8F5EC), false, false);
            }
        }
        if (mon.getLength() == 0) agregar(mon, "(todavía ningún programa imprimió con INT 10H)", new Color(0x6B8A77), false, true);
        areaMonitor.setCaretPosition(mon.getLength());

        StyledDocument doc = areaConsola.getStyledDocument();
        areaConsola.setText("");
        if (gestor != null) {
            for (String linea : gestor.getConsola()) {
                if (linea.startsWith("ERROR")) agregar(doc, linea + "\n", Tema.ROJO, true, false);
                else if (linea.startsWith(">>")) agregar(doc, linea + "\n", Tema.AMBAR, true, false);
                else if (linea.startsWith("[SO]")) agregar(doc, linea + "\n", Tema.TEXTO_SUAVE, false, true);
                else if (linea.startsWith("P")) agregar(doc, linea + "\n", Tema.VERDE_OSCURO, true, false);
                else agregar(doc, linea + "\n", Tema.AZUL, false, false);
            }
        }
        areaConsola.setCaretPosition(doc.getLength());
    }

    private void agregar(StyledDocument doc, String texto, Color color, boolean negrita, boolean cursiva){
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, Tema.MONO_NORMAL.getFamily());
        StyleConstants.setFontSize(a, 13);
        StyleConstants.setForeground(a, color);
        StyleConstants.setBold(a, negrita);
        StyleConstants.setItalic(a, cursiva);
        try {
            doc.insertString(doc.getLength(), texto, a);
        } catch (BadLocationException ignorada) { }
    }
}
