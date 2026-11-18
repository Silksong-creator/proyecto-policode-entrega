import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Lógica de negocio del sistema de inventario (Java). */
public class Inventario {
    private final Map<String, Producto> productos = new LinkedHashMap<>();
    private final List<Movimiento> movimientos = new ArrayList<>();

    public Inventario(Collection<Producto> iniciales) {
        if (iniciales != null) iniciales.forEach(this::agregarProducto);
    }

    // ---------- Productos ----------
    public void agregarProducto(Producto p) {
        if (productos.containsKey(p.getCodigo()))
            throw new IllegalArgumentException("Ya existe un producto con código " + p.getCodigo());
        productos.put(p.getCodigo(), p);
    }

    /** Modifica los campos permitidos de un producto existente. */
    public Producto modificarProducto(String codigo, Map<String, Object> cambios) {
        Producto p = obtener(codigo);
        for (Map.Entry<String, Object> e : cambios.entrySet()) {
            switch (e.getKey()) {
                case "nombre", "categoria" -> {
                    if (!(e.getValue() instanceof String texto) || texto.isBlank())
                        throw new IllegalArgumentException("El campo " + e.getKey() + " es obligatorio.");
                }
                case "precio" -> {
                    if (!(e.getValue() instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() < 0)
                        throw new IllegalArgumentException("El precio debe ser un número no negativo.");
                }
                case "stock_minimo" -> {
                    if (!(e.getValue() instanceof Number n) || n.doubleValue() < 0 || n.doubleValue() != n.intValue())
                        throw new IllegalArgumentException("El stock mínimo debe ser un entero no negativo.");
                }
                default -> throw new IllegalArgumentException("Campo no modificable: " + e.getKey());
            }
        }
        for (Map.Entry<String, Object> e : cambios.entrySet()) {
            switch (e.getKey()) {
                case "nombre" -> p.setNombre((String) e.getValue());
                case "categoria" -> p.setCategoria((String) e.getValue());
                case "precio" -> p.setPrecio(((Number) e.getValue()).doubleValue());
                case "stock_minimo" -> p.setStockMinimo(((Number) e.getValue()).intValue());
                default -> throw new IllegalArgumentException("Campo no modificable: " + e.getKey());
            }
        }
        return p;
    }

    /** Devuelve el producto o null si no existe. */
    public Producto consultarPorCodigo(String codigo) { return productos.get(codigo); }

    /** Busca por nombre (parcial) y/o categoría. Streams = filtrado declarativo. */
    public List<Producto> buscar(String texto, String categoria) {
        final String t = texto == null ? "" : texto.toLowerCase();
        final String c = categoria == null ? "" : categoria.toLowerCase();
        Predicate<Producto> filtro = p -> p.getNombre().toLowerCase().contains(t)
                && p.getCategoria().toLowerCase().contains(c);
        return productos.values().stream().filter(filtro).collect(Collectors.toList());
    }

    public List<Producto> todos() { return new ArrayList<>(productos.values()); }

    // ---------- Movimientos ----------
    public void registrarEntrada(String codigo, int cantidad, String fecha) {
        Producto p = obtener(codigo);
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        p.setExistencia(p.getExistencia() + cantidad);
        movimientos.add(new Movimiento(codigo, "entrada", cantidad, fecha));
    }

    public void registrarSalida(String codigo, int cantidad, String fecha) {
        Producto p = obtener(codigo);
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        // Regla de negocio: nunca permitir salidas superiores al inventario
        if (cantidad > p.getExistencia())
            throw new IllegalArgumentException(String.format(
                "Salida inválida: se solicitaron %d unidades de %s pero solo hay %d disponibles.",
                cantidad, p.getNombre(), p.getExistencia()));
        p.setExistencia(p.getExistencia() - cantidad);
        movimientos.add(new Movimiento(codigo, "salida", cantidad, fecha));
    }

    /** Procesa varios movimientos; si uno falla, no quedan cambios parciales. */
    public void procesarLote(List<Movimiento> lote) {
        Map<String, Integer> copia = new HashMap<>();
        productos.forEach((k, v) -> copia.put(k, v.getExistencia()));
        int marcador = movimientos.size();
        try {
            for (Movimiento m : lote) {
                if (m.getTipo().equals("entrada")) registrarEntrada(m.getCodigoProducto(), m.getCantidad(), m.getFecha());
                else registrarSalida(m.getCodigoProducto(), m.getCantidad(), m.getFecha());
            }
        } catch (RuntimeException e) {
            copia.forEach((k, v) -> productos.get(k).setExistencia(v));
            movimientos.subList(marcador, movimientos.size()).clear();
            throw e;
        }
    }

    /** Historial de movimientos, opcionalmente filtrado por producto. */
    public List<Movimiento> historial(String codigo) {
        if (codigo == null) return new ArrayList<>(movimientos);
        return movimientos.stream()
                .filter(m -> m.getCodigoProducto().equals(codigo))
                .collect(Collectors.toList());
    }

    // ---------- Reglas de negocio y reportes ----------
    public double valorTotal() {
        return Math.round(productos.values().stream()
                .mapToDouble(Producto::getValorTotal).sum() * 100.0) / 100.0;
    }

    public List<Producto> alertasBajoInventario() {
        return productos.values().stream()
                .filter(p -> p.isBajoInventario() && !p.isAgotado())
                .collect(Collectors.toList());
    }

    public List<Producto> agotados() {
        return productos.values().stream().filter(Producto::isAgotado).collect(Collectors.toList());
    }

    public List<Producto> disponibles() {
        return productos.values().stream().filter(p -> !p.isAgotado()).collect(Collectors.toList());
    }

    /** Ordena por precio, existencia o categoria. */
    public List<Producto> ordenar(String criterio, boolean inverso) {
        Comparator<Producto> cmp = switch (criterio) {
            case "precio" -> Comparator.comparingDouble(Producto::getPrecio);
            case "existencia" -> Comparator.comparingInt(Producto::getExistencia);
            case "categoria" -> Comparator.comparing(Producto::getCategoria);
            default -> throw new IllegalArgumentException(
                "Criterio inválido. Use 'precio', 'existencia' o 'categoria'.");
        };
        if (inverso) cmp = cmp.reversed();
        return productos.values().stream().sorted(cmp).collect(Collectors.toList());
    }

    public List<Producto> reporteMayorValor(Integer limite) {
        List<Producto> ordenados = productos.values().stream()
                .sorted(Comparator.comparingDouble(Producto::getValorTotal).reversed())
                .collect(Collectors.toList());
        return limite == null ? ordenados : ordenados.subList(0, Math.min(limite, ordenados.size()));
    }

    private Producto obtener(String codigo) {
        Producto p = productos.get(codigo);
        if (p == null) throw new NoSuchElementException("No existe producto con código " + codigo);
        return p;
    }
}
