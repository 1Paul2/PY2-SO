package minipc.gui;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/**
 * Tema: paleta de colores y fuentes de la interfaz. Fondo blanco con acentos
 * verdes; cada estado de proceso y cada proceso en memoria tiene su color.
 */
public final class Tema {

    private Tema() {}

    // ---- Base ----
    public static final Color FONDO = new Color(0xF3F6F4);
    public static final Color TARJETA = Color.WHITE;
    public static final Color BORDE = new Color(0xDDE5E0);
    public static final Color TEXTO = new Color(0x1F2A24);
    public static final Color TEXTO_SUAVE = new Color(0x6B7A72);
    public static final Color FILA_ALTERNA = new Color(0xF7FAF8);

    // ---- Acento verde ----
    public static final Color VERDE = new Color(0x16A34A);
    public static final Color VERDE_OSCURO = new Color(0x15803D);
    public static final Color VERDE_CLARO = new Color(0xDCFCE7);
    public static final Color VERDE_MEDIO = new Color(0x86EFAC);

    // ---- Colores de apoyo ----
    public static final Color AZUL = new Color(0x2563EB);
    public static final Color AZUL_CLARO = new Color(0xDBEAFE);
    public static final Color AMBAR = new Color(0xD97706);
    public static final Color AMBAR_CLARO = new Color(0xFEF3C7);
    public static final Color ROJO = new Color(0xDC2626);
    public static final Color ROJO_CLARO = new Color(0xFEE2E2);
    public static final Color MORADO = new Color(0x7C3AED);
    public static final Color MORADO_CLARO = new Color(0xEDE9FE);
    public static final Color GRIS = new Color(0x64748B);
    public static final Color GRIS_CLARO = new Color(0xF1F5F9);
    public static final Color NARANJA_SO = new Color(0xF59E0B);

    // Un color por proceso (se repite si hay más de 8)
    private static final List<Color> COLORES_PROCESO = Arrays.asList(
        new Color(0x22C55E), new Color(0x3B82F6), new Color(0xA855F7), new Color(0x14B8A6),
        new Color(0xEC4899), new Color(0xF97316), new Color(0x6366F1), new Color(0x84CC16)
    );

    // ---- Fuentes ----
    private static final String SANS = elegirFuente("Segoe UI", "Inter", "Helvetica Neue", "SansSerif");
    private static final String MONO = elegirFuente("JetBrains Mono", "Consolas", "Menlo", "Monospaced");

    public static final Font TITULO = new Font(SANS, Font.BOLD, 20);
    public static final Font SUBTITULO = new Font(SANS, Font.PLAIN, 12);
    public static final Font TITULO_CARD = new Font(SANS, Font.BOLD, 14);
    public static final Font NORMAL = new Font(SANS, Font.PLAIN, 13);
    public static final Font NEGRITA = new Font(SANS, Font.BOLD, 13);
    public static final Font PEQUENA = new Font(SANS, Font.PLAIN, 11);
    public static final Font PEQUENA_B = new Font(SANS, Font.BOLD, 11);
    public static final Font MONO_NORMAL = new Font(MONO, Font.PLAIN, 13);
    public static final Font MONO_GRANDE = new Font(MONO, Font.BOLD, 18);

    private static String elegirFuente(String... opciones){
        List<String> disponibles = Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : opciones) {
            if (disponibles.contains(f)) return f;
        }
        return opciones[opciones.length - 1];
    }

    // E: idProceso (int)
    // S: Color - color fijo que identifica a ese proceso en la memoria y las tablas
    // R: ninguna
    public static Color colorProceso(int idProceso){
        return COLORES_PROCESO.get(Math.floorMod(idProceso - 1, COLORES_PROCESO.size()));
    }

    // E: estado (String) - estado del proceso
    // S: Color[] - {color del texto/borde, color de fondo} para el distintivo del estado
    // R: ninguna
    // Correccion - Poll (observación PY1): "Finalizado con error" se pinta en rojo
    public static Color[] coloresEstado(String estado){
        if (estado == null) return new Color[]{GRIS, GRIS_CLARO};
        switch (estado) {
            case "Nuevo":return new Color[]{GRIS, GRIS_CLARO};
            case "Preparado":return new Color[]{AZUL, AZUL_CLARO};
            case "Ejecución":return new Color[]{VERDE_OSCURO, VERDE_CLARO};
            case "En espera":return new Color[]{AMBAR, AMBAR_CLARO};
            case "Finalizado":return new Color[]{new Color(0x475569), new Color(0xE2E8F0)};
            case "Finalizado con error":return new Color[]{ROJO, ROJO_CLARO};
            default:return new Color[]{GRIS, GRIS_CLARO};
        }
    }

    // E: c (Color), factor (float) - 0..1, cuánto aclarar hacia blanco
    // S: Color - versión más clara del color
    // R: ninguna
    public static Color aclarar(Color c, float factor){
        int r = (int) (c.getRed() + (255 - c.getRed()) * factor);
        int g = (int) (c.getGreen() + (255 - c.getGreen()) * factor);
        int b = (int) (c.getBlue() + (255 - c.getBlue()) * factor);
        return new Color(r, g, b);
    }
}
