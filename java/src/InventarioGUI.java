import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.nio.file.Path;

/** Interfaz gráfica (Swing) del sistema de inventario (Java).
 *  Reutiliza la misma clase Inventario de la lógica de negocio.
 *  Ejecutar: java -cp ../src InventarioGUI */
public class InventarioGUI extends JFrame {
    private final Inventario inv;
    private final JTable tabla;
    private final DefaultTableModel modelo;
    private final JLabel lblTotal = new JLabel();
    private final JTextField txtBuscar = new JTextField(12);
    private final JTextField txtCategoria = new JTextField(10);
    private final JComboBox<String> cmbOrden = new JComboBox<>(
        new String[]{"codigo", "precio", "existencia", "categoria", "valor"});
    private final JRadioButton rbTodos = new JRadioButton("Todos", true);
    private final JRadioButton rbDisponibles = new JRadioButton("Disponibles");
    private final JRadioButton rbAgotados = new JRadioButton("Agotados");
    private final JRadioButton rbBajo = new JRadioButton("Bajo inventario");
    private final JRadioButton rbMayor = new JRadioButton("Mayor valor");
    private final JComboBox<String> cmbExportar = new JComboBox<>(new String[]{"JSON", "CSV"});

    private static final String[] COLUMNAS =
        {"Código", "Nombre", "Categoría", "Precio", "Existencia", "Stock mín.", "Valor"};

    public InventarioGUI(Inventario inv) {
        super("Sistema de Control de Inventario — Java/Swing");
        this.inv = inv;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // --- barra de acciones ---
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton bNuevo = new JButton("＋ Nuevo producto");
        JButton bModif = new JButton("✏ Modificar");
        JButton bEntrada = new JButton("⬇ Entrada");
        JButton bSalida = new JButton("⬆ Salida");
        JButton bHistorial = new JButton("🕘 Historial");
        JButton bExportar = new JButton("Exportar");
        acciones.add(bNuevo); acciones.add(bModif); acciones.add(bEntrada);
        acciones.add(bSalida); acciones.add(bHistorial);
        acciones.add(cmbExportar); acciones.add(bExportar);
        bNuevo.addActionListener(e -> nuevoProducto());
        bModif.addActionListener(e -> modificarProducto());
        bEntrada.addActionListener(e -> movimiento("entrada"));
        bSalida.addActionListener(e -> movimiento("salida"));
        bHistorial.addActionListener(e -> historial());
        bExportar.addActionListener(e -> exportar());

        // --- filtros y reportes ---
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup grupo = new ButtonGroup();
        for (JRadioButton rb : new JRadioButton[]{rbTodos, rbDisponibles, rbAgotados, rbBajo, rbMayor}) {
            grupo.add(rb); filtros.add(rb);
            rb.addActionListener(e -> refrescar());
        }
        filtros.add(new JLabel("Buscar:")); filtros.add(txtBuscar);
        filtros.add(new JLabel("Categoría:")); filtros.add(txtCategoria);
        filtros.add(new JLabel("Ordenar por:")); filtros.add(cmbOrden);
        txtBuscar.addCaretListener(e -> refrescar());
        txtCategoria.addCaretListener(e -> refrescar());
        cmbOrden.addActionListener(e -> refrescar());

        // --- tabla ---
        modelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // --- pie ---
        JPanel pie = new JPanel(new BorderLayout());
        pie.add(lblTotal, BorderLayout.EAST);
        pie.add(new JLabel("🟡 bajo inventario · seleccione una fila y use los botones"), BorderLayout.WEST);

        setLayout(new BorderLayout());
        JPanel norte = new JPanel(new GridLayout(2, 1));
        norte.add(acciones);
        norte.add(filtros);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);

        refrescar();
    }

    private String codigoSeleccionado() {
        int f = tabla.getSelectedRow();
        if (f < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto en la tabla.");
            return null;
        }
        return (String) modelo.getValueAt(tabla.convertRowIndexToModel(f), 0);
    }

    private List<Producto> datos() {
        List<Producto> datos = new java.util.ArrayList<>(inv.buscar(txtBuscar.getText(), txtCategoria.getText()));
        if (rbDisponibles.isSelected()) datos.removeIf(p -> p.isAgotado());
        else if (rbAgotados.isSelected()) datos.removeIf(p -> !p.isAgotado());
        else if (rbBajo.isSelected()) datos.removeIf(p -> !p.isBajoInventario() || p.isAgotado());
        else if (rbMayor.isSelected()) {
            List<Producto> mayor = inv.reporteMayorValor(null);
            datos.removeIf(p -> !mayor.contains(p));
        }
        String crit = (String) cmbOrden.getSelectedItem();
        if ("valor".equals(crit) || (rbMayor.isSelected() && "codigo".equals(crit)))
            datos.sort(java.util.Comparator.comparingDouble(Producto::getValorTotal).reversed());
        else if ("codigo".equals(crit)) datos.sort(java.util.Comparator.comparing(Producto::getCodigo));
        else datos = inv.ordenar(crit, false).stream().filter(datos::contains).toList();
        return datos;
    }

    private void refrescar() {
        modelo.setRowCount(0);
        for (Producto p : datos())
            modelo.addRow(new Object[]{p.getCodigo(), p.getNombre(), p.getCategoria(),
                String.format("%.2f", p.getPrecio()), p.getExistencia(),
                p.getStockMinimo(), String.format("%.2f", p.getValorTotal())});
        lblTotal.setText(String.format("Valor total del inventario: $%,.2f", inv.valorTotal()));
    }

    private void nuevoProducto() {
        try {
            String codigo = pedir("Código:");
            if (codigo == null) return;
            String nombre = pedir("Nombre:");
            if (nombre == null) return;
            String categoria = pedir("Categoría (vacío = General):");
            if (categoria == null) return;
            String precio = pedir("Precio:");
            if (precio == null) return;
            String existencia = pedir("Existencia:");
            if (existencia == null) return;
            String minimo = pedir("Stock mínimo:");
            if (minimo == null) return;
            inv.agregarProducto(new Producto(codigo, nombre,
                categoria.isBlank() ? "General" : categoria,
                Double.parseDouble(precio), Integer.parseInt(existencia),
                Integer.parseInt(minimo)));
        } catch (Exception e) { error(e); }
        refrescar();
    }

    private void modificarProducto() {
        String codigo = codigoSeleccionado();
        if (codigo == null) return;
        Producto p = inv.consultarPorCodigo(codigo);
        try {
            String nombre = (String) JOptionPane.showInputDialog(this, "Nombre:", "Modificar",
                JOptionPane.PLAIN_MESSAGE, null, null, p.getNombre());
            if (nombre == null) return;
            String categoria = (String) JOptionPane.showInputDialog(this, "Categoría:", "Modificar",
                JOptionPane.PLAIN_MESSAGE, null, null, p.getCategoria());
            if (categoria == null) return;
            String precioTexto = (String) JOptionPane.showInputDialog(
                this, "Precio:", "Modificar", JOptionPane.PLAIN_MESSAGE, null, null, p.getPrecio());
            if (precioTexto == null) return;
            String minimoTexto = (String) JOptionPane.showInputDialog(
                this, "Stock mínimo:", "Modificar", JOptionPane.PLAIN_MESSAGE, null, null, p.getStockMinimo());
            if (minimoTexto == null) return;
            double precio = Double.parseDouble(precioTexto);
            int smin = Integer.parseInt(minimoTexto);
            inv.modificarProducto(codigo, java.util.Map.of("nombre", nombre, "categoria", categoria,
                "precio", precio, "stock_minimo", smin));
        } catch (Exception e) { error(e); }
        refrescar();
    }

    private void movimiento(String tipo) {
        String codigo = codigoSeleccionado();
        if (codigo == null) return;
        try {
            String valor = pedir("Cantidad (entero positivo):");
            if (valor == null) return;
            int cantidad = Integer.parseInt(valor);
            String fecha = LocalDate.now().toString();
            if (tipo.equals("entrada")) inv.registrarEntrada(codigo, cantidad, fecha);
            else inv.registrarSalida(codigo, cantidad, fecha);
        } catch (Exception e) { error(e); }  // p. ej. salida excesiva
        refrescar();
    }

    private void historial() {
        String codigo = codigoSeleccionado();
        if (codigo == null) return;
        StringBuilder sb = new StringBuilder();
        inv.historial(codigo).forEach(m -> sb.append(String.format("%s  %-8s  %d%n",
            m.getFecha(), m.getTipo().toUpperCase(), m.getCantidad())));
        JOptionPane.showMessageDialog(this,
            sb.length() == 0 ? "(sin movimientos)" : sb.toString(), "Historial " + codigo,
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(Path.of("productos_exportados." +
            ((String) cmbExportar.getSelectedItem()).toLowerCase()).toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            String ruta = chooser.getSelectedFile().getAbsolutePath();
            if ("JSON".equals(cmbExportar.getSelectedItem()))
                Persistencia.guardarProductosJson(inv.todos(), ruta);
            else Persistencia.guardarProductosCsv(inv.todos(), ruta);
            JOptionPane.showMessageDialog(this, "Archivo exportado:\n" + ruta);
        } catch (IOException e) { error(e); }
    }

    private String pedir(String mensaje) {
        return JOptionPane.showInputDialog(this, mensaje);
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Operación rechazada",
            JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) throws Exception {
        Inventario inv = new Inventario(Persistencia.cargarProductosCsv(RutaDatos.archivo("productos.csv").toString()));
        SwingUtilities.invokeLater(() -> new InventarioGUI(inv).setVisible(true));
    }
}
