package com.mycompany.usocrud;

import servicio.OrdenServicio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class Main extends JFrame {

    private final EntityManagerFactory emf = Persistence.createEntityManagerFactory("compumaibPU");

    // ----- Campos del formulario -----
    private final JTextField txtCliente = new JTextField();
    private final JTextField txtEquipo = new JTextField();
    private final JTextField txtProcesador = new JTextField();
    private final JTextField txtRam = new JTextField();
    private final JTextField txtAlmacenamiento = new JTextField();
    private final JTextField txtFalla = new JTextField();
    private final JComboBox<String> cmbEstado = new JComboBox<>(
            new String[]{"RECIBIDA", "EN_REPARACION", "FINALIZADA", "ENTREGADA"});
    private final JTextField txtCosto = new JTextField();

    // ----- Botones -----
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnInsertar = new JButton("Insertar");
    private final JButton btnActualizar = new JButton("Actualizar");
    private final JButton btnEliminar = new JButton("Eliminar");

    // ----- Tablade Datos -----
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Cliente", "Equipo", "Procesador", "RAM", "Almacenamiento",
                "Falla", "Estado", "Fecha ingreso", "Costo"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);

    // null = no hay orden seleccionada (se está creando una nueva)
    private Integer idSeleccionado = null;

    public Main() {
        super("COMPUMAIB - Órdenes de servicio técnico");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Formulario 1
        JPanel form = new JPanel(new GridLayout(0, 4, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Datos de la orden"));
        form.add(new JLabel("Cliente:"));
        form.add(txtCliente);
        form.add(new JLabel("Equipo (marca y modelo):"));
        form.add(txtEquipo);
        form.add(new JLabel("Procesador:"));
        form.add(txtProcesador);
        form.add(new JLabel("RAM (ej. 16GB):"));
        form.add(txtRam);
        form.add(new JLabel("Almacenamiento (ej. SSD 500GB):"));
        form.add(txtAlmacenamiento);
        form.add(new JLabel("Falla / servicio:"));
        form.add(txtFalla);
        form.add(new JLabel("Estado:"));
        form.add(cmbEstado);
        form.add(new JLabel("Costo ($):"));
        form.add(txtCosto);
        add(form, BorderLayout.NORTH);

        // Tabla (centro)
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(24);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Botones abajo
        JPanel botones = new JPanel();
        botones.add(btnNuevo);
        botones.add(btnInsertar);
        botones.add(btnActualizar);
        botones.add(btnEliminar);
        add(botones, BorderLayout.SOUTH);

        // Eventos
        btnNuevo.addActionListener(e -> nuevo());
        btnInsertar.addActionListener(e -> insertar());
        btnActualizar.addActionListener(e -> actualizar());
        btnEliminar.addActionListener(e -> eliminar());

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarFilaEnFormulario();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                emf.close();
            }
        });

        cargarTabla();
        nuevo();
    }

    // ---------- Utilidades ----------
    private void transaccion(Consumer<EntityManager> accion) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                accion.accept(em);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                em.getTransaction().rollback();
                throw e;
            }
        }
    }

    private void mensaje(String texto) {
        JOptionPane.showMessageDialog(this, texto);
    }

    private void error(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void modoEdicion(boolean editando) {
        btnInsertar.setEnabled(!editando);
        btnActualizar.setEnabled(editando);
        btnEliminar.setEnabled(editando);
    }

    // Valida el formulario y ver si hay error
    private Double validar() {
        if (txtCliente.getText().isBlank() || txtEquipo.getText().isBlank()) {
            error("El cliente y el equipo son obligatorios.");
            return null;
        }
        try {
            String texto = txtCosto.getText().isBlank() ? "0" : txtCosto.getText().trim();
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            error("El costo debe ser un número (ejemplo: 75.50).");
            return null;
        }
    }

    // ---------- Tabla de datos ----------
    private void cargarTabla() {
        modelo.setRowCount(0);
        try (EntityManager em = emf.createEntityManager()) {
            List<OrdenServicio> ordenes = em
                    .createQuery("SELECT o FROM OrdenServicio o ORDER BY o.id", OrdenServicio.class)
                    .getResultList();
            for (OrdenServicio o : ordenes) {
                modelo.addRow(new Object[]{o.getId(), o.getCliente(), o.getEquipo(),
                    o.getProcesador(), o.getRam(), o.getAlmacenamiento(), o.getFalla(),
                    o.getEstado(), o.getFechaIngreso(), o.getCosto()});
            }
        }
    }

    private void cargarFilaEnFormulario() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        idSeleccionado = (Integer) modelo.getValueAt(fila, 0);
        txtCliente.setText(String.valueOf(modelo.getValueAt(fila, 1)));
        txtEquipo.setText(String.valueOf(modelo.getValueAt(fila, 2)));
        txtProcesador.setText(textoODefecto(modelo.getValueAt(fila, 3)));
        txtRam.setText(textoODefecto(modelo.getValueAt(fila, 4)));
        txtAlmacenamiento.setText(textoODefecto(modelo.getValueAt(fila, 5)));
        txtFalla.setText(textoODefecto(modelo.getValueAt(fila, 6)));
        cmbEstado.setSelectedItem(modelo.getValueAt(fila, 7));
        txtCosto.setText(textoODefecto(modelo.getValueAt(fila, 9)));
        modoEdicion(true);
    }

    private String textoODefecto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    // ---------- Operaciones ----------
    private void nuevo() {
        idSeleccionado = null;
        tabla.clearSelection();
        txtCliente.setText("");
        txtEquipo.setText("");
        txtProcesador.setText("");
        txtRam.setText("");
        txtAlmacenamiento.setText("");
        txtFalla.setText("");
        cmbEstado.setSelectedIndex(0);
        txtCosto.setText("");
        modoEdicion(false);
        txtCliente.requestFocus();
    }

    private void insertar() {
        Double costo = validar();
        if (costo == null) {
            return;
        }
        try {
            OrdenServicio o = new OrdenServicio(
                    txtCliente.getText().trim(), txtEquipo.getText().trim(),
                    txtProcesador.getText().trim(), txtRam.getText().trim(),
                    txtAlmacenamiento.getText().trim(), txtFalla.getText().trim(),
                    (String) cmbEstado.getSelectedItem(), costo);
            transaccion(em -> em.persist(o));
            cargarTabla();
            nuevo();
            mensaje("¡Orden guardada exitosamente!");
        } catch (RuntimeException e) {
            error("No se pudo guardar: " + e.getMessage());
        }
    }

    private void actualizar() {
        if (idSeleccionado == null) {
            error("Seleccione una orden de la tabla.");
            return;
        }
        Double costo = validar();
        if (costo == null) {
            return;
        }
        try {
            transaccion(em -> {
                OrdenServicio o = em.find(OrdenServicio.class, idSeleccionado);
                o.setCliente(txtCliente.getText().trim());
                o.setEquipo(txtEquipo.getText().trim());
                o.setProcesador(txtProcesador.getText().trim());
                o.setRam(txtRam.getText().trim());
                o.setAlmacenamiento(txtAlmacenamiento.getText().trim());
                o.setFalla(txtFalla.getText().trim());
                o.setEstado((String) cmbEstado.getSelectedItem());
                o.setCosto(costo);
            });
            cargarTabla();
            nuevo();
            mensaje("¡Orden actualizada!");
        } catch (RuntimeException e) {
            error("No se pudo actualizar: " + e.getMessage());
        }
    }

    private void eliminar() {
        if (idSeleccionado == null) {
            error("Seleccione una orden de la tabla.");
            return;
        }
        int resp = JOptionPane.showConfirmDialog(this,
                "¿Eliminar la orden #" + idSeleccionado + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (resp != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            transaccion(em -> {
                OrdenServicio o = em.find(OrdenServicio.class, idSeleccionado);
                if (o != null) {
                    em.remove(o);
                }
            });
            cargarTabla();
            nuevo();
            mensaje("¡Orden eliminada!");
        } catch (RuntimeException e) {
            error("No se pudo eliminar: " + e.getMessage());
        }
    }

    // ---------- Programa principal ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}