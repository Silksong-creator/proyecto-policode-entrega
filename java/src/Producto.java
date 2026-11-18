/** Representa un producto del inventario (Java). */
public class Producto {
    private final String codigo;
    private String nombre;
    private String categoria;
    private double precio;
    private int existencia;
    private int stockMinimo;

    public Producto(String codigo, String nombre, String categoria,
                    double precio, int existencia, int stockMinimo) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("El código del producto es obligatorio.");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        if (!Double.isFinite(precio) || precio < 0) throw new IllegalArgumentException("El precio debe ser un número no negativo.");
        if (existencia < 0) throw new IllegalArgumentException("La existencia no puede ser negativa.");
        if (stockMinimo < 0) throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
        this.codigo = codigo.trim();
        this.nombre = nombre.trim();
        this.categoria = categoria.trim();
        this.precio = precio;
        this.existencia = existencia;
        this.stockMinimo = stockMinimo;
    }

    /** Valor monetario almacenado de este producto. */
    public double getValorTotal() { return Math.round(precio * existencia * 100.0) / 100.0; }
    public boolean isAgotado() { return existencia == 0; }
    /** Alerta: existencia menor o igual al stock mínimo. */
    public boolean isBajoInventario() { return existencia <= stockMinimo; }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public double getPrecio() { return precio; }
    public int getExistencia() { return existencia; }
    public int getStockMinimo() { return stockMinimo; }

    void setExistencia(int existencia) { this.existencia = existencia; }
    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        this.nombre = nombre.trim();
    }
    public void setCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) throw new IllegalArgumentException("La categoría es obligatoria.");
        this.categoria = categoria.trim();
    }
    public void setPrecio(double precio) {
        if (!Double.isFinite(precio) || precio < 0) throw new IllegalArgumentException("El precio debe ser un número no negativo.");
        this.precio = precio;
    }
    public void setStockMinimo(int stockMinimo) {
        if (stockMinimo < 0) throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
        this.stockMinimo = stockMinimo;
    }

    @Override
    public String toString() {
        return String.format("Producto(%s, %s, existencia=%d)", codigo, nombre, existencia);
    }
}
