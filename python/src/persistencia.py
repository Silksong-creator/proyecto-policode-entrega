"""Carga y guardado de datos en JSON y CSV."""
import csv
import json

from models import Producto


def cargar_productos_json(ruta):
    """Carga productos desde un archivo JSON. Lanza IOError si falla."""
    try:
        with open(ruta, encoding="utf-8") as f:
            return [Producto.from_dict(p) for p in json.load(f)]
    except (OSError, json.JSONDecodeError, KeyError, ValueError) as e:
        raise IOError(f"No se pudieron cargar los productos desde {ruta}: {e}") from e


def guardar_productos_json(productos, ruta):
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump([p.to_dict() for p in productos], f, indent=2, ensure_ascii=False)


def cargar_productos_csv(ruta):
    productos = []
    try:
        with open(ruta, newline="", encoding="utf-8") as f:
            for fila in csv.DictReader(f):
                productos.append(Producto.from_dict(fila))
        return productos
    except (OSError, KeyError, ValueError) as e:
        raise IOError(f"No se pudieron cargar los productos desde {ruta}: {e}") from e


def guardar_productos_csv(productos, ruta):
    campos = ["codigo", "nombre", "categoria", "precio", "existencia", "stock_minimo"]
    with open(ruta, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=campos)
        w.writeheader()
        for p in productos:
            w.writerow(p.to_dict())


def cargar_movimientos_json(ruta):
    try:
        with open(ruta, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        raise IOError(f"No se pudieron cargar los movimientos desde {ruta}: {e}") from e


def guardar_movimientos_json(movimientos, ruta):
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump([m.to_dict() for m in movimientos], f, indent=2, ensure_ascii=False)
