"""Pruebas automatizadas del sistema de inventario (unittest)."""
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from inventario import Inventario
from models import Producto, Movimiento
from persistencia import (cargar_productos_json, guardar_productos_json,
                          cargar_productos_csv, guardar_productos_csv)


def inventario_demo():
    return Inventario([
        Producto("P001", "Teclado USB", "Perifericos", 25.50, 40, 10),
        Producto("P002", "Mouse Inalambrico", "Perifericos", 15.75, 5, 10),
        Producto("P004", "Laptop Pro 14", "Computo", 950.00, 0, 2),
    ])


class TestProductos(unittest.TestCase):
    def test_registrar_y_consultar_por_codigo(self):
        inv = inventario_demo()
        self.assertEqual(inv.consultar_por_codigo("P001").nombre, "Teclado USB")
        self.assertIsNone(inv.consultar_por_codigo("ZZZ"))

    def test_no_permitir_codigo_duplicado(self):
        inv = inventario_demo()
        with self.assertRaises(ValueError):
            inv.agregar_producto(Producto("P001", "Otro", "X", 1.0, 1, 1))

    def test_modificar_producto(self):
        inv = inventario_demo()
        p = inv.modificar_producto("P001", precio=30.0, nombre="Teclado Mecanico")
        self.assertEqual(p.precio, 30.0)
        self.assertEqual(p.nombre, "Teclado Mecanico")
        with self.assertRaises(ValueError):
            inv.modificar_producto("P001", nombre="Cambio parcial", precio=float("nan"))
        self.assertEqual(p.nombre, "Teclado Mecanico")
        with self.assertRaises(KeyError):
            inv.modificar_producto("ZZZ", precio=1)

    def test_buscar_por_nombre_y_categoria(self):
        inv = inventario_demo()
        self.assertEqual(len(inv.buscar(texto="teclado")), 1)
        self.assertEqual(len(inv.buscar(categoria="perifericos")), 2)
        self.assertEqual(len(inv.buscar(texto="mouse", categoria="perifericos")), 1)

    def test_validacion_datos_invalidos(self):
        with self.assertRaises(ValueError):
            Producto("", "X", "X", 1, 1, 1)
        with self.assertRaises(ValueError):
            Producto("P1", "X", "X", -5, 1, 1)
        with self.assertRaises(ValueError):
            Producto("P1", "X", "X", float("nan"), 1, 1)
        with self.assertRaises(ValueError):
            Producto("P1", "X", "X", 1, 1.5, 1)
        with self.assertRaises(ValueError):
            Movimiento("P1", "ajuste", 3, "2026-01-01")


class TestMovimientos(unittest.TestCase):
    def test_entrada_actualiza_existencia(self):
        inv = inventario_demo()
        inv.registrar_entrada("P001", 10, "2026-11-10")
        self.assertEqual(inv.consultar_por_codigo("P001").existencia, 50)
        self.assertEqual(len(inv.historial()), 1)

    def test_salida_valida_actualiza_existencia(self):
        inv = inventario_demo()
        inv.registrar_salida("P001", 5, "2026-11-10")
        self.assertEqual(inv.consultar_por_codigo("P001").existencia, 35)

    def test_salida_mayor_a_existencia_es_rechazada(self):
        inv = inventario_demo()
        with self.assertRaises(ValueError):
            inv.registrar_salida("P002", 99, "2026-11-10")
        # existencia intacta tras el rechazo
        self.assertEqual(inv.consultar_por_codigo("P002").existencia, 5)

    def test_salida_producto_inexistente(self):
        inv = inventario_demo()
        with self.assertRaises(KeyError):
            inv.registrar_salida("ZZZ", 1, "2026-11-10")

    def test_procesar_lote_y_historial_filtrado(self):
        inv = inventario_demo()
        inv.procesar_lote([
            {"codigo_producto": "P001", "tipo": "entrada", "cantidad": 5, "fecha": "2026-11-10"},
            {"codigo_producto": "P001", "tipo": "salida", "cantidad": 3, "fecha": "2026-11-11"},
        ])
        self.assertEqual(inv.consultar_por_codigo("P001").existencia, 42)
        self.assertEqual(len(inv.historial("P001")), 2)

    def test_lote_fallido_no_deja_cambios_parciales(self):
        inv = inventario_demo()
        with self.assertRaises(ValueError):
            inv.procesar_lote([
                {"codigo_producto": "P001", "tipo": "entrada", "cantidad": 5, "fecha": "2026-11-10"},
                {"codigo_producto": "P002", "tipo": "salida", "cantidad": 99, "fecha": "2026-11-10"},
            ])
        self.assertEqual(inv.consultar_por_codigo("P001").existencia, 40)
        self.assertEqual(len(inv.historial()), 0)


class TestReportes(unittest.TestCase):
    def test_valor_total(self):
        inv = inventario_demo()
        esperado = round(25.50 * 40 + 15.75 * 5 + 950.00 * 0, 2)
        self.assertEqual(inv.valor_total(), esperado)

    def test_alertas_bajo_inventario(self):
        inv = inventario_demo()
        codigos = {p.codigo for p in inv.alertas_bajo_inventario()}
        self.assertEqual(codigos, {"P002"})  # P004 está agotado, no "bajo"

    def test_agotados_y_disponibles(self):
        inv = inventario_demo()
        self.assertEqual([p.codigo for p in inv.agotados()], ["P004"])
        self.assertEqual(len(inv.disponibles()), 2)

    def test_ordenar_por_criterios(self):
        inv = inventario_demo()
        self.assertEqual([p.codigo for p in inv.ordenar("precio")], ["P002", "P001", "P004"])
        self.assertEqual([p.codigo for p in inv.ordenar("existencia", inverso=True)],
                         ["P001", "P002", "P004"])

    def test_reporte_mayor_valor(self):
        inv = inventario_demo()
        self.assertEqual(inv.reporte_mayor_valor(1)[0].codigo, "P001")


class TestPersistencia(unittest.TestCase):
    def test_redonde_json_y_csv(self):
        inv = inventario_demo()
        with tempfile.TemporaryDirectory() as carpeta:
            tmp_json = os.path.join(carpeta, "productos.json")
            tmp_csv = os.path.join(carpeta, "productos.csv")
            guardar_productos_json(inv.todos(), tmp_json)
            guardar_productos_csv(inv.todos(), tmp_csv)
            self.assertEqual([p.to_dict() for p in cargar_productos_json(tmp_json)],
                             [p.to_dict() for p in cargar_productos_csv(tmp_csv)])

    def test_cargar_archivo_inexistente_lanza_ioerror(self):
        with self.assertRaises(IOError):
            cargar_productos_json(os.path.join(tempfile.gettempdir(), "inventario-no-existe.json"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
