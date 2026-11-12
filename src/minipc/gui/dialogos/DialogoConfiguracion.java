package minipc.gui.dialogos;

/**
 *
 * @author Poll Anthony Garro Vargas - 2024129001
 * @Universidad: Instituto Tecnológico de Costa Rica
 *
 */

import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import minipc.gui.Componentes;
import minipc.gui.Componentes.Boton;
import minipc.gui.Componentes.Tarjeta;
import minipc.gui.Tema;
import minipc.hardware.EstrategiaMemoria;
import minipc.modelo.BCP;
import minipc.modelo.GestorProcesos;
import minipc.util.ConfiguracionSistema;

/**
 * DialogoConfiguracion: ventana con dos pestañas. "CPU y tamaños" elige la cantidad de CPU,
 * el algoritmo de planificación con sus parámetros y los tamaños de memoria y disco;
 * "Asignación de memoria" elige la estrategia y sus parámetros. Guarda en config.txt.
 */
public final class DialogoConfiguracion {

    private DialogoConfiguracion() {}

    // E: padre (Frame); configuracion (ConfiguracionSistema) - se modifica si el usuario guarda
    // S: boolean - true si se guardó y aplicó la configuración, false si se canceló
    // R: ninguna
    // Correccion - Poll: la zona del S.O. y la memoria virtual se pueden editar; ya no se
    // calculan con porcentajes fijos (25 % y 12,5 %)
    public static boolean mostrar(Frame padre, ConfiguracionSistema configuracion){
        boolean[] aplicada = {false};
        JDialog dlg = new JDialog(padre, "Configuración del Mini PC", true);
        Tarjeta t = new Tarjeta("Tamaños (se guardan en " + ConfiguracionSistema.ARCHIVO_POR_DEFECTO + ")", Tema.VERDE);
        JPanel campos = new JPanel(new GridLayout(9, 2, 10, 10));
        campos.setOpaque(false);
        JSpinner cantidadCpus = new JSpinner(new SpinnerNumberModel(configuracion.getCantidadCpus(), 1, GestorProcesos.MAX_CPUS, 1));
        campos.add(Componentes.etiqueta("Cantidad de CPU (1 a 5)")); campos.add(cantidadCpus);
        JComboBox<String> algoritmoCpu = new JComboBox<>(new String[]{"FCFS", "SRT", "SPN", "RR", "HRRN", "SRR", "Lottery", "Prioridad", "Guaranteed", "MLFQ"});
        algoritmoCpu.setSelectedItem(configuracion.getAlgoritmoCpu());
        campos.add(Componentes.etiqueta("Algoritmo de CPU")); campos.add(algoritmoCpu);
        JSpinner quantumRR = new JSpinner(new SpinnerNumberModel(configuracion.getQuantumRR(), 1, Integer.MAX_VALUE, 1));
        quantumRR.setEnabled(("RR".equals(algoritmoCpu.getSelectedItem()) || "SRR".equals(algoritmoCpu.getSelectedItem()) || "Lottery".equals(algoritmoCpu.getSelectedItem()) || "MLFQ".equals(algoritmoCpu.getSelectedItem())));
        algoritmoCpu.addActionListener(e -> quantumRR.setEnabled(("RR".equals(algoritmoCpu.getSelectedItem()) || "SRR".equals(algoritmoCpu.getSelectedItem()) || "Lottery".equals(algoritmoCpu.getSelectedItem()) || "MLFQ".equals(algoritmoCpu.getSelectedItem()))));
        campos.add(Componentes.etiqueta("Quantum RR / SRR / Lottery / base MLFQ (s)")); campos.add(quantumRR);
        JSpinner nivelesMLFQ = new JSpinner(new SpinnerNumberModel(configuracion.getNivelesMLFQ(), 2, 8, 1));
        JSpinner boostMLFQ = new JSpinner(new SpinnerNumberModel(configuracion.getPeriodoBoostMLFQ(), 1, Integer.MAX_VALUE, 1));
        Runnable habilitarMLFQ = () -> {
            boolean seleccionado = "MLFQ".equals(algoritmoCpu.getSelectedItem());
            nivelesMLFQ.setEnabled(seleccionado);
            boostMLFQ.setEnabled(seleccionado);
        };
        algoritmoCpu.addActionListener(e -> habilitarMLFQ.run());
        habilitarMLFQ.run();
        campos.add(Componentes.etiqueta("Niveles MLFQ (2 a 8)")); campos.add(nivelesMLFQ);
        campos.add(Componentes.etiqueta("Promoción global MLFQ (s)")); campos.add(boostMLFQ);
        JTextField mp = campo(configuracion.getMemoriaPrincipal());
        JTextField ms = campo(configuracion.getMemoriaSistema());
        JTextField as = campo(configuracion.getAlmacenamientoSecundario());
        JTextField mv = campo(configuracion.getMemoriaVirtual());
        campos.add(Componentes.etiqueta("Memoria principal"));               campos.add(mp);
        campos.add(Componentes.etiqueta("Zona del S.O."));                   campos.add(ms);
        campos.add(Componentes.etiqueta("Almacenamiento secundario"));       campos.add(as);
        campos.add(Componentes.etiqueta("Memoria virtual (en disco)"));      campos.add(mv);
        JComboBox<EstrategiaMemoria> estrategiaMemoria = new JComboBox<>(EstrategiaMemoria.values());
        estrategiaMemoria.setSelectedItem(configuracion.getEstrategiaMemoria());
        JTextField tamanoPagina = campo(configuracion.getTamanoPagina());
        JTextField tamanoParticion = campo(configuracion.getTamanoParticion());
        JTextField particiones = new JTextField(configuracion.getListaParticiones());
        JPanel asignacion = new JPanel(new GridLayout(4, 2, 10, 10));
        asignacion.setOpaque(false);
        asignacion.add(Componentes.etiqueta("Estrategia")); asignacion.add(estrategiaMemoria);
        asignacion.add(Componentes.etiqueta("Tamaño de página (posiciones)")); asignacion.add(tamanoPagina);
        asignacion.add(Componentes.etiqueta("Tamaño de partición igual")); asignacion.add(tamanoParticion);
        asignacion.add(Componentes.etiqueta("Particiones diferentes (ej. 16,32,64)")); asignacion.add(particiones);
        Runnable habilitarMemoria = () -> {
            EstrategiaMemoria estrategia = (EstrategiaMemoria) estrategiaMemoria.getSelectedItem();
            tamanoPagina.setEnabled(estrategia.esPaginada());
            tamanoParticion.setEnabled(estrategia == EstrategiaMemoria.FIJA_IGUAL);
            particiones.setEnabled(estrategia == EstrategiaMemoria.FIJA_VARIABLE);
        };
        estrategiaMemoria.addActionListener(e -> habilitarMemoria.run());
        habilitarMemoria.run();
        JPanel panelAsignacion = new JPanel(new BorderLayout(0, 12));
        panelAsignacion.setOpaque(false);
        panelAsignacion.add(asignacion, BorderLayout.NORTH);
        JLabel nota = Componentes.etiqueta("<html>Los tamaños usan posiciones de memoria. Cada línea ocupa una posición.<br>"
            + "Paginación virtual usa FIFO y requiere un marco en RAM.<br>"
            + "Los restos de páginas y particiones se reservan como fragmentación interna.<br>"
            + "La zona sobrante que no forma una partición o marco completo queda sin asignar.</html>");
        panelAsignacion.add(nota, BorderLayout.CENTER);
        JTabbedPane pestanasConfiguracion = new JTabbedPane();
        pestanasConfiguracion.addTab("CPU y tamaños", campos);
        pestanasConfiguracion.addTab("Asignación de memoria", panelAsignacion);
        t.getContenido().add(pestanasConfiguracion, BorderLayout.CENTER);
        JLabel capacidad = Componentes.etiqueta("");
        Runnable mostrarCapacidad = () -> {
            int n = ((Number) cantidadCpus.getValue()).intValue();
            capacidad.setText("Hasta " + (5 * n) + " procesos; " + (5 * n * BCP.TAMANO_BLOQUE)
                + " posiciones de S.O. para sus BCP (además de la RAM de usuario).");
        };
        cantidadCpus.addChangeListener(e -> mostrarCapacidad.run());
        mostrarCapacidad.run();
        t.getContenido().add(capacidad, BorderLayout.SOUTH);

        Boton cargar = new Boton("Cargar archivo...", Tema.GRIS);
        Boton aplicar = new Boton("Guardar y aplicar", Tema.VERDE);
        Boton cancelar = new Boton("Cancelar", new Color(0x94A3B8));

        cargar.addActionListener(e -> {
            JFileChooser sel = new JFileChooser(new File("."));
            if (sel.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                try {
                    configuracion.cargarDesdeArchivo(sel.getSelectedFile());
                    cantidadCpus.setValue(configuracion.getCantidadCpus());
                    algoritmoCpu.setSelectedItem(configuracion.getAlgoritmoCpu());
                    quantumRR.setValue(configuracion.getQuantumRR());
                    nivelesMLFQ.setValue(configuracion.getNivelesMLFQ());
                    boostMLFQ.setValue(configuracion.getPeriodoBoostMLFQ());
                    mp.setText(String.valueOf(configuracion.getMemoriaPrincipal()));
                    ms.setText(String.valueOf(configuracion.getMemoriaSistema()));
                    as.setText(String.valueOf(configuracion.getAlmacenamientoSecundario()));
                    mv.setText(String.valueOf(configuracion.getMemoriaVirtual()));
                    estrategiaMemoria.setSelectedItem(configuracion.getEstrategiaMemoria());
                    tamanoPagina.setText(String.valueOf(configuracion.getTamanoPagina()));
                    tamanoParticion.setText(String.valueOf(configuracion.getTamanoParticion()));
                    particiones.setText(configuracion.getListaParticiones());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        aplicar.addActionListener(e -> {
            try {
                cantidadCpus.commitEdit();
                if (quantumRR.isEnabled()) quantumRR.commitEdit();
                if (nivelesMLFQ.isEnabled()) {
                    nivelesMLFQ.commitEdit();
                    boostMLFQ.commitEdit();
                }
                configuracion.setConfiguracionMemoria(
                    Integer.parseInt(mp.getText().trim()), Integer.parseInt(ms.getText().trim()),
                    Integer.parseInt(as.getText().trim()), Integer.parseInt(mv.getText().trim()),
                    (EstrategiaMemoria) estrategiaMemoria.getSelectedItem(),
                    Integer.parseInt(tamanoPagina.getText().trim()), Integer.parseInt(tamanoParticion.getText().trim()),
                    particiones.getText());
                configuracion.setCantidadCpus(((Number) cantidadCpus.getValue()).intValue());
                configuracion.setAlgoritmoCpu((String) algoritmoCpu.getSelectedItem());
                configuracion.setQuantumRR((Integer) quantumRR.getValue());
                configuracion.setParametrosMLFQ((Integer) nivelesMLFQ.getValue(), (Integer) boostMLFQ.getValue());
                configuracion.guardarEnArchivo(new File(ConfiguracionSistema.ARCHIVO_POR_DEFECTO));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Los cuatro tamaños deben ser números enteros.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            aplicada[0] = true;
            dlg.dispose();
        });
        cancelar.addActionListener(e -> dlg.dispose());

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pie.setOpaque(false);
        pie.add(cargar);
        pie.add(cancelar);
        pie.add(aplicar);

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Tema.FONDO);
        raiz.setBorder(new EmptyBorder(16, 16, 16, 16));
        raiz.add(t, BorderLayout.CENTER);
        raiz.add(pie, BorderLayout.SOUTH);
        dlg.setContentPane(raiz);
        dlg.pack();
        dlg.setLocationRelativeTo(padre);
        dlg.setVisible(true);   // modal: espera hasta que se cierre
        return aplicada[0];
    }

    private static JTextField campo(int valor){
        JTextField f = new JTextField(String.valueOf(valor), 8);
        f.setFont(Tema.MONO_NORMAL);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.BORDE), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }
}
