import java.util.List;

/** Interfaz de línea de comandos del sistema de inventario (Java). */
public class Main {
    public static void main(String[] args) throws Exception {
        Inventario inv = new Inventario(Persistencia.cargarProductosCsv(RutaDatos.archivo("productos.csv").toString()));
        mostrar("Todos los productos", inv.todos());
        mostrar("Disponibles", inv.disponibles());
        mostrar("Agotados", inv.agotados());
        mostrar("Bajo inventario (ALERTA)", inv.alertasBajoInventario());
        mostrar("Mayor valor", inv.reporteMayorValor(3));
        System.out.printf("%nValor total del inventario: $%.2f%n", inv.valorTotal());

        // Ejemplo: procesar un lote de movimientos
        inv.procesarLote(List.of(
            new Movimiento("P001", "entrada", 10, "2026-11-10"),
            new Movimiento("P002", "salida", 2, "2026-11-10")));
        System.out.println("Historial P001: " + inv.historial("P001"));
        Persistencia.guardarProductosJson(inv.todos(), RutaDatos.archivo("productos.csv").getParent()
                .resolve("productos_exportados_java.json").toString());
        System.out.println("Datos exportados a productos_exportados_java.json");
    }

    static void mostrar(String titulo, List<Producto> lista) {
        System.out.printf("%n== %s ==%n", titulo);
        if (lista.isEmpty()) System.out.println("  (sin resultados)");
        for (Producto p : lista)
            System.out.printf("  %s | %-20s | %-14s | $%8.2f | exist: %4d | valor: $%10.2f%n",
                p.getCodigo(), p.getNombre(), p.getCategoria(),
                p.getPrecio(), p.getExistencia(), p.getValorTotal());
    }
}
