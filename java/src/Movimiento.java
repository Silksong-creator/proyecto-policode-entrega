/** Registro de una entrada o salida de mercancía (Java). */
public class Movimiento {
    private final String codigoProducto;
    private final String tipo;
    private final int cantidad;
    private final String fecha;

    public Movimiento(String codigoProducto, String tipo, int cantidad, String fecha) {
        if (!tipo.equals("entrada") && !tipo.equals("salida"))
            throw new IllegalArgumentException("Tipo de movimiento inválido: " + tipo);
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser un entero positivo.");
        this.codigoProducto = codigoProducto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.fecha = fecha;
    }

    public String getCodigoProducto() { return codigoProducto; }
    public String getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public String getFecha() { return fecha; }

    @Override
    public String toString() {
        return String.format("Movimiento(%s, %s, %d)", codigoProducto, tipo, cantidad);
    }
}
