"""Lógica de negocio del sistema de inventario (Python)."""
import math

from models import Movimiento, Producto


class Inventario:
    """Gestiona productos, movimientos y reglas de negocio."""

    def __init__(self, productos=None):
        self._productos = {}
        for p in productos or []:
            self.agregar_producto(p)
        self._movimientos = []

    # ---------- Productos ----------
    def agregar_producto(self, producto):
        if producto.codigo in self._productos:
            raise ValueError(f"Ya existe un producto con código {producto.codigo}.")
        self._productos[producto.codigo] = producto

    def modificar_producto(self, codigo, **cambios):
        """Modifica los campos permitidos de un producto existente."""
        if codigo not in self._productos:
            raise KeyError(f"No existe producto con código {codigo}.")
        p = self._productos[codigo]
        for campo, valor in cambios.items():
            if campo not in ("nombre", "categoria", "precio", "stock_minimo"):
                raise ValueError(f"Campo no modificable: {campo}.")
            if campo in ("nombre", "categoria") and (not isinstance(valor, str) or not valor.strip()):
                raise ValueError(f"El campo {campo} es obligatorio.")
            if campo == "precio" and (not isinstance(valor, (int, float))
                                       or not math.isfinite(valor) or valor < 0):
                raise ValueError("El precio debe ser un número no negativo.")
            if campo == "stock_minimo" and (isinstance(valor, bool)
                    or not isinstance(valor, int) or valor < 0):
                raise ValueError("El stock mínimo debe ser un entero no negativo.")
        for campo, valor in cambios.items():
            setattr(p, campo, valor)
        return p

    def consultar_por_codigo(self, codigo):
        """Devuelve el producto o None si no existe."""
        return self._productos.get(codigo)

    def buscar(self, texto="", categoria=""):
        """Busca por nombre (parcial) y/o categoría exacta."""
        texto, categoria = texto.lower(), categoria.lower()
        # Comprensión de lista + funciones lambda: filtrado declarativo
        return [
            p for p in self._productos.values()
            if texto in p.nombre.lower() and categoria in p.categoria.lower()
        ]

    def todos(self):
        return list(self._productos.values())

    # ---------- Movimientos ----------
    def registrar_entrada(self, codigo, cantidad, fecha):
        p = self._obtener(codigo)
        movimiento = Movimiento(codigo, "entrada", cantidad, fecha)
        p.existencia += cantidad
        self._movimientos.append(movimiento)

    def registrar_salida(self, codigo, cantidad, fecha):
        p = self._obtener(codigo)
        movimiento = Movimiento(codigo, "salida", cantidad, fecha)
        if cantidad > p.existencia:
            # Regla de negocio: nunca permitir salidas superiores al inventario
            raise ValueError(
                f"Salida inválida: se solicitaron {cantidad} unidades de "
                f"{p.nombre} pero solo hay {p.existencia} disponibles."
            )
        p.existencia -= cantidad
        self._movimientos.append(movimiento)

    def procesar_lote(self, movimientos):
        """Procesa varios movimientos (lista de dicts); todo o nada."""
        resultado, copia = [], {c: p.existencia for c, p in self._productos.items()}
        marcador = len(self._movimientos)
        try:
            for m in movimientos:
                if m["tipo"] == "entrada":
                    self.registrar_entrada(m["codigo_producto"], m["cantidad"], m["fecha"])
                elif m["tipo"] == "salida":
                    self.registrar_salida(m["codigo_producto"], m["cantidad"], m["fecha"])
                else:
                    raise ValueError(f"Tipo desconocido: {m['tipo']}")
                resultado.append(m)
        except Exception:
            for c, e in copia.items():
                self._productos[c].existencia = e
            del self._movimientos[marcador:]
            raise
        return resultado

    def historial(self, codigo=None):
        """Historial de movimientos, opcionalmente filtrado por producto."""
        if codigo is None:
            return list(self._movimientos)
        # Función de orden superior (filter + lambda)
        return list(filter(lambda m: m.codigo_producto == codigo, self._movimientos))

    # ---------- Reglas de negocio y reportes ----------
    def valor_total(self):
        # Función de orden superior (map + lambda)
        return round(sum(map(lambda p: p.valor_total, self._productos.values())), 2)

    def alertas_bajo_inventario(self):
        return [p for p in self._productos.values() if p.bajo_inventario and not p.agotado]

    def agotados(self):
        return [p for p in self._productos.values() if p.agotado]

    def disponibles(self):
        return [p for p in self._productos.values() if not p.agotado]

    def ordenar(self, criterio="precio", inverso=False):
        """Ordena por precio, existencia o categoria."""
        if criterio not in ("precio", "existencia", "categoria"):
            raise ValueError("Criterio inválido. Use 'precio', 'existencia' o 'categoria'.")
        return sorted(self._productos.values(), key=lambda p: getattr(p, criterio),
                      reverse=inverso)

    def reporte_mayor_valor(self, limite=None):
        ordenados = sorted(self._productos.values(),
                           key=lambda p: p.valor_total, reverse=True)
        return ordenados if limite is None else ordenados[:limite]

    def _obtener(self, codigo):
        if codigo not in self._productos:
            raise KeyError(f"No existe producto con código {codigo}.")
        return self._productos[codigo]
