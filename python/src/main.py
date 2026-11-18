"""Interfaz de línea de comandos del sistema de inventario (Python)."""
from inventario import Inventario
from persistencia import cargar_productos_json, guardar_productos_json

RUTA_PRODUCTOS = "../datos/productos.json"
RUTA_SALIDA = "../datos/productos_exportados.json"


def mostrar_lista(titulo, lista):
    print(f"\n== {titulo} ==")
    if not lista:
        print("  (sin resultados)")
    for p in lista:
        print(f"  {p.codigo} | {p.nombre:<20} | {p.categoria:<14} | "
              f"${p.precio:>8.2f} | exist: {p.existencia:>4} | valor: ${p.valor_total:>10.2f}")


def menu():
    inventario = Inventario(cargar_productos_json(RUTA_PRODUCTOS))
    while True:
        print("\n--- SISTEMA DE INVENTARIO ---")
        print("1. Listar productos   2. Disponibles        3. Agotados")
        print("4. Alertas stock      5. Valor total        6. Mayor valor")
        print("7. Ordenar            0. Salir")
        opcion = input("Opción: ").strip()
        try:
            if opcion == "0":
                guardar_productos_json(inventario.todos(), RUTA_SALIDA)
                print("Datos exportados a", RUTA_SALIDA)
                return
            acciones = {
                "1": lambda: mostrar_lista("Todos los productos", inventario.todos()),
                "2": lambda: mostrar_lista("Disponibles", inventario.disponibles()),
                "3": lambda: mostrar_lista("Agotados", inventario.agotados()),
                "4": lambda: mostrar_lista("Bajo inventario (ALERTA)", inventario.alertas_bajo_inventario()),
                "5": lambda: print(f"\nValor total del inventario: ${inventario.valor_total():.2f}"),
                "6": lambda: mostrar_lista("Mayor valor", inventario.reporte_mayor_valor()),
            }
            if opcion == "7":
                criterio = input("Criterio (precio/existencia/categoria): ").strip()
                mostrar_lista(f"Ordenados por {criterio}", inventario.ordenar(criterio))
            elif opcion in acciones:
                acciones[opcion]()
            else:
                print("Opción inválida.")
        except (ValueError, KeyError) as error:
            print("Error:", error)


if __name__ == "__main__":
    menu()
