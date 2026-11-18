import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Pruebas automatizadas sin dependencias externas (Java).
 *  Ejecutar: java -cp ../src:. InventarioTest  (desde tests/, ya compilado con src) */
public class InventarioTest {
    static int pasados = 0, fallidos = 0;

    static void afirmar(boolean condicion, String nombre) {
        if (condicion) { pasados++; System.out.println("ok - " + nombre); }
        else { fallidos++; System.out.println("FALLA - " + nombre); }
    }

    static Inventario demo() {
        return new Inventario(List.of(
            new Producto("P001", "Teclado USB", "Perifericos", 25.50, 40, 10),
            new Producto("P002", "Mouse Inalambrico", "Perifericos", 15.75, 5, 10),
            new Producto("P004", "Laptop Pro 14", "Computo", 950.00, 0, 2)));
    }

    public static void main(String[] args) {
        registrarYConsultar();
        noPermitirDuplicado();
        modificarProducto();
        buscar();
        validacion();
        entradaActualiza();
        salidaMayorEsRechazada();
        loteYRollback();
        valorTotal();
        alertasYAgotados();
        ordenar();
        mayorValor();
        persistenciaCsv();
        System.out.printf("%n%d pasados, %d fallidos%n", pasados, fallidos);
        if (fallidos > 0) System.exit(1);
    }

    static void registrarYConsultar() {
        Inventario inv = demo();
        afirmar(inv.consultarPorCodigo("P001").getNombre().equals("Teclado USB"), "consultar por código");
        afirmar(inv.consultarPorCodigo("ZZZ") == null, "código inexistente devuelve null");
    }

    static void noPermitirDuplicado() {
        Inventario inv = demo();
        try { inv.agregarProducto(new Producto("P001", "Otro", "X", 1, 1, 1));
              afirmar(false, "duplicado lanza excepción"); }
        catch (IllegalArgumentException e) { afirmar(true, "duplicado lanza excepción"); }
    }

    static void modificarProducto() {
        Inventario inv = demo();
        Producto p = inv.modificarProducto("P001", Map.of("precio", 30.0, "nombre", "Teclado Mecanico"));
        afirmar(p.getPrecio() == 30.0 && p.getNombre().equals("Teclado Mecanico"), "modificar producto");
        try { inv.modificarProducto("P001", Map.of("nombre", "Cambio parcial", "precio", Double.NaN)); }
        catch (IllegalArgumentException e) { }
        afirmar(p.getNombre().equals("Teclado Mecanico"), "modificación inválida no deja cambios parciales");
        try { inv.modificarProducto("ZZZ", Map.of("precio", 1.0));
              afirmar(false, "modificar inexistente lanza excepción"); }
        catch (NoSuchElementException e) { afirmar(true, "modificar inexistente lanza excepción"); }
    }

    static void buscar() {
        Inventario inv = demo();
        afirmar(inv.buscar("teclado", "").size() == 1, "buscar por nombre");
        afirmar(inv.buscar("", "perifericos").size() == 2, "buscar por categoría");
        afirmar(inv.buscar("mouse", "perifericos").size() == 1, "buscar nombre+categoría");
    }

    static void validacion() {
        boolean ok = false;
        try { new Producto("", "X", "X", 1, 1, 1); } catch (IllegalArgumentException e) { ok = true; }
        afirmar(ok, "código vacío rechazado");
        ok = false;
        try { new Movimiento("P1", "ajuste", 3, "2026-01-01"); } catch (IllegalArgumentException e) { ok = true; }
        afirmar(ok, "tipo de movimiento inválido rechazado");
    }

    static void entradaActualiza() {
        Inventario inv = demo();
        inv.registrarEntrada("P001", 10, "2026-11-10");
        afirmar(inv.consultarPorCodigo("P001").getExistencia() == 50, "entrada actualiza existencia");
        afirmar(inv.historial(null).size() == 1, "historial registrado");
    }

    static void salidaMayorEsRechazada() {
        Inventario inv = demo();
        try { inv.registrarSalida("P002", 99, "2026-11-10");
              afirmar(false, "salida excesiva rechazada"); }
        catch (IllegalArgumentException e) {
            afirmar(inv.consultarPorCodigo("P002").getExistencia() == 5, "salida excesiva rechazada");
        }
    }

    static void loteYRollback() {
        Inventario inv = demo();
        try {
            inv.procesarLote(List.of(
                new Movimiento("P001", "entrada", 5, "2026-11-10"),
                new Movimiento("P002", "salida", 99, "2026-11-10")));
            afirmar(false, "lote fallido hace rollback");
        } catch (IllegalArgumentException e) {
            afirmar(inv.consultarPorCodigo("P001").getExistencia() == 40
                 && inv.historial(null).isEmpty(), "lote fallido hace rollback");
        }
    }

    static void valorTotal() {
        Inventario inv = demo();
        double esperado = Math.round((25.50 * 40 + 15.75 * 5) * 100.0) / 100.0;
        afirmar(inv.valorTotal() == esperado, "valor total del inventario");
    }

    static void alertasYAgotados() {
        Inventario inv = demo();
        afirmar(inv.alertasBajoInventario().size() == 1
             && inv.alertasBajoInventario().get(0).getCodigo().equals("P002"), "alertas bajo inventario");
        afirmar(inv.agotados().size() == 1 && inv.disponibles().size() == 2, "agotados y disponibles");
    }

    static void ordenar() {
        Inventario inv = demo();
        List<Producto> porPrecio = inv.ordenar("precio", false);
        afirmar(porPrecio.get(0).getCodigo().equals("P002"), "ordenar por precio");
        afirmar(inv.ordenar("existencia", true).get(0).getCodigo().equals("P001"), "ordenar por existencia");
    }

    static void mayorValor() {
        Inventario inv = demo();
        afirmar(inv.reporteMayorValor(1).get(0).getCodigo().equals("P001"), "mayor valor");
    }

    static void persistenciaCsv() {
        try {
            Inventario inv = demo();
            File temporal = File.createTempFile("inventario-java-", ".csv");
            Persistencia.guardarProductosCsv(inv.todos(), temporal.getAbsolutePath());
            List<Producto> cargados = Persistencia.cargarProductosCsv(temporal.getAbsolutePath());
            afirmar(cargados.size() == 3
                 && cargados.get(0).getCodigo().equals("P001"), "CSV redondo");
            temporal.delete();
        } catch (Exception e) { afirmar(false, "CSV redondo: " + e); }
    }
}
