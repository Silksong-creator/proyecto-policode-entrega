"""Modelos de datos del sistema de inventario (Python)."""
import math


class Producto:
    """Representa un producto del inventario."""

    def __init__(self, codigo, nombre, categoria, precio, existencia, stock_minimo):
        if not codigo or not codigo.strip():
            raise ValueError("El código del producto es obligatorio.")
        if not nombre or not nombre.strip():
            raise ValueError("El nombre del producto es obligatorio.")
        try:
            precio = float(precio)
        except (TypeError, ValueError) as exc:
            raise ValueError("El precio debe ser numérico.") from exc
        if not math.isfinite(precio) or precio < 0:
            raise ValueError("El precio debe ser un número no negativo.")
        if isinstance(existencia, bool) or not isinstance(existencia, int) or existencia < 0:
            raise ValueError("La existencia debe ser un entero no negativo.")
        if isinstance(stock_minimo, bool) or not isinstance(stock_minimo, int) or stock_minimo < 0:
            raise ValueError("El stock mínimo debe ser un entero no negativo.")
        self.codigo = codigo.strip()
        self.nombre = nombre.strip()
        self.categoria = categoria.strip()
        self.precio = precio
        self.existencia = existencia
        self.stock_minimo = stock_minimo

    @property
    def valor_total(self):
        """Valor monetario almacenado de este producto."""
        return round(self.precio * self.existencia, 2)

    @property
    def agotado(self):
        return self.existencia == 0

    @property
    def bajo_inventario(self):
        """Alerta: existencia menor o igual al stock mínimo."""
        return self.existencia <= self.stock_minimo

    def to_dict(self):
        return {
            "codigo": self.codigo,
            "nombre": self.nombre,
            "categoria": self.categoria,
            "precio": self.precio,
            "existencia": self.existencia,
            "stock_minimo": self.stock_minimo,
        }

    @staticmethod
    def from_dict(d):
        return Producto(
            d["codigo"], d["nombre"], d["categoria"],
            float(d["precio"]), int(d["existencia"]), int(d["stock_minimo"]),
        )

    def __repr__(self):
        return f"Producto({self.codigo!r}, {self.nombre!r}, existencia={self.existencia})"


class Movimiento:
    """Registro de una entrada o salida de mercancía."""

    TIPOS_VALIDOS = ("entrada", "salida")

    def __init__(self, codigo_producto, tipo, cantidad, fecha):
        if tipo not in Movimiento.TIPOS_VALIDOS:
            raise ValueError(f"Tipo de movimiento inválido: {tipo!r}. Use 'entrada' o 'salida'.")
        if isinstance(cantidad, bool) or not isinstance(cantidad, int) or cantidad <= 0:
            raise ValueError("La cantidad debe ser un entero positivo.")
        self.codigo_producto = codigo_producto
        self.tipo = tipo
        self.cantidad = cantidad
        self.fecha = fecha

    def to_dict(self):
        return {
            "codigo_producto": self.codigo_producto,
            "tipo": self.tipo,
            "cantidad": self.cantidad,
            "fecha": self.fecha,
        }

    def __repr__(self):
        return f"Movimiento({self.codigo_producto!r}, {self.tipo!r}, {self.cantidad})"
