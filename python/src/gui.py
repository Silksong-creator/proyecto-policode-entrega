"""Interfaz gráfica (tkinter) del sistema de inventario (Python).

Reutiliza la misma lógica de negocio de inventario.py; solo cambia la capa de presentación.
"""
import tkinter as tk
from tkinter import messagebox, simpledialog, ttk
from datetime import date
from pathlib import Path

from inventario import Inventario
from models import Producto
from persistencia import (cargar_productos_json, guardar_productos_json,
                          guardar_productos_csv)

RAIZ_PROYECTO = Path(__file__).resolve().parents[2]
RUTA_PRODUCTOS = RAIZ_PROYECTO / "datos" / "productos.json"
RUTA_EXPORTACIONES = RAIZ_PROYECTO / "datos"


class InventarioGUI(tk.Tk):
    COLUMNAS = ("codigo", "nombre", "categoria", "precio", "existencia", "stock_minimo", "valor")

    def __init__(self, inventario):
        super().__init__()
        self.title("Sistema de Control de Inventario - Python/tkinter")
        self.geometry("980x560")
        self.inv = inventario
        self._crear_widgets()
        self._refrescar()

    def _crear_widgets(self):
        barra = ttk.Frame(self)
        barra.pack(fill="x", padx=8, pady=6)
        for texto, cmd in [("Nuevo producto", self._nuevo_producto),
                           ("Modificar", self._modificar_producto),
                           ("Entrada", lambda: self._movimiento("entrada")),
                           ("Salida", lambda: self._movimiento("salida"))]:
            ttk.Button(barra, text=texto, command=cmd).pack(side="left", padx=2)

        ttk.Label(barra, text="   Buscar:").pack(side="left")
        self.var_buscar = tk.StringVar()
        self.var_buscar.trace_add("write", lambda *_: self._refrescar())
        ttk.Entry(barra, textvariable=self.var_buscar, width=18).pack(side="left", padx=2)
        ttk.Label(barra, text="Categoria:").pack(side="left")
        self.var_cat = tk.StringVar()
        self.var_cat.trace_add("write", lambda *_: self._refrescar())
        ttk.Entry(barra, textvariable=self.var_cat, width=12).pack(side="left", padx=2)

        filtro = ttk.Frame(self)
        filtro.pack(fill="x", padx=8)
        self.var_reporte = tk.StringVar(value="todos")
        for clave, texto in [("todos", "Todos"), ("disponibles", "Disponibles"),
                             ("agotados", "Agotados"), ("bajo", "Bajo inventario"),
                             ("mayor_valor", "Mayor valor")]:
            ttk.Radiobutton(filtro, text=texto, value=clave,
                            variable=self.var_reporte, command=self._refrescar).pack(side="left")
        ttk.Label(filtro, text="   Ordenar por:").pack(side="left", padx=(12, 2))
        self.var_orden = tk.StringVar(value="codigo")
        combo = ttk.Combobox(filtro, textvariable=self.var_orden, width=10, state="readonly",
                             values=["codigo", "precio", "existencia", "categoria"])
        combo.pack(side="left")
        combo.bind("<<ComboboxSelected>>", lambda *_: self._refrescar())

        tabla_frame = ttk.Frame(self)
        tabla_frame.pack(fill="both", expand=True, padx=8, pady=6)
        self.tabla = ttk.Treeview(tabla_frame, columns=self.COLUMNAS, show="headings")
        for c in self.COLUMNAS:
            self.tabla.heading(c, text=c.replace("_", " ").title())
            self.tabla.column(c, width=110 if c != "nombre" else 180, anchor="center")
        scroll_y = ttk.Scrollbar(tabla_frame, orient="vertical", command=self.tabla.yview)
        scroll_x = ttk.Scrollbar(tabla_frame, orient="horizontal", command=self.tabla.xview)
        self.tabla.configure(yscrollcommand=scroll_y.set, xscrollcommand=scroll_x.set)
        self.tabla.grid(row=0, column=0, sticky="nsew")
        scroll_y.grid(row=0, column=1, sticky="ns")
        scroll_x.grid(row=1, column=0, sticky="ew")
        tabla_frame.rowconfigure(0, weight=1)
        tabla_frame.columnconfigure(0, weight=1)

        abajo = ttk.Frame(self)
        abajo.pack(fill="x", padx=8, pady=4)
        ttk.Button(abajo, text="Historial del seleccionado", command=self._historial).pack(side="left", padx=2)
        ttk.Button(abajo, text="Exportar JSON", command=lambda: self._exportar("json")).pack(side="left", padx=2)
        ttk.Button(abajo, text="Exportar CSV", command=lambda: self._exportar("csv")).pack(side="left", padx=2)
        self.var_total = tk.StringVar()
        ttk.Label(abajo, textvariable=self.var_total, font=("", 10, "bold")).pack(side="right")

    def _seleccionado(self):
        sel = self.tabla.selection()
        if not sel:
            messagebox.showinfo("Aviso", "Seleccione un producto en la tabla.")
            return None
        return self.tabla.item(sel[0], "values")[0]

    def _refrescar(self):
        codigo_seleccionado = self._codigo_seleccionado_sin_aviso()
        for fila in self.tabla.get_children():
            self.tabla.delete(fila)
        reporte = self.var_reporte.get()
        datos = self.inv.buscar(self.var_buscar.get(), self.var_cat.get())
        filtros = {
            "disponibles": {p.codigo for p in self.inv.disponibles()},
            "agotados": {p.codigo for p in self.inv.agotados()},
            "bajo": {p.codigo for p in self.inv.alertas_bajo_inventario()},
            "mayor_valor": {p.codigo for p in self.inv.reporte_mayor_valor()},
        }
        if reporte in filtros:
            codigos = filtros[reporte]
            datos = [p for p in datos if p.codigo in codigos]
        criterio = self.var_orden.get()
        if criterio != "codigo":
            datos = sorted(datos, key=lambda p: getattr(p, criterio))
        for p in datos:
            iid = self.tabla.insert("", "end", iid=p.codigo, values=(p.codigo, p.nombre, p.categoria,
                                f"{p.precio:.2f}", p.existencia, p.stock_minimo, f"{p.valor_total:.2f}"))
            if p.codigo == codigo_seleccionado:
                self.tabla.selection_set(iid)
        if codigo_seleccionado and codigo_seleccionado not in {p.codigo for p in datos}:
            self.tabla.selection_remove(codigo_seleccionado)
        self.var_total.set(f"Valor total del inventario: ${self.inv.valor_total():,.2f}")

    def _codigo_seleccionado_sin_aviso(self):
        seleccion = self.tabla.selection()
        return seleccion[0] if seleccion else None

    def _pedir(self, titulo, campos):
        valores = {}
        for campo, defecto in campos:
            v = simpledialog.askstring(titulo, f"{campo}:", initialvalue=defecto, parent=self)
            if v is None:
                return None
            valores[campo] = v
        return valores

    def _nuevo_producto(self):
        d = self._pedir("Nuevo producto", [("codigo", ""), ("nombre", ""), ("categoria", ""),
                                           ("precio", "0"), ("existencia", "0"), ("stock_minimo", "0")])
        if not d:
            return
        try:
            self.inv.agregar_producto(Producto(d["codigo"], d["nombre"], d["categoria"],
                                               float(d["precio"]), int(d["existencia"]), int(d["stock_minimo"])))
        except (ValueError, KeyError) as e:
            messagebox.showerror("Error", str(e))
        self._refrescar()

    def _modificar_producto(self):
        codigo = self._seleccionado()
        if not codigo:
            return
        p = self.inv.consultar_por_codigo(codigo)
        d = self._pedir(f"Modificar {codigo}", [("nombre", p.nombre), ("categoria", p.categoria),
                                                ("precio", str(p.precio)), ("stock_minimo", str(p.stock_minimo))])
        if not d:
            return
        try:
            self.inv.modificar_producto(codigo, nombre=d["nombre"], categoria=d["categoria"],
                                        precio=float(d["precio"]), stock_minimo=int(d["stock_minimo"]))
        except (ValueError, KeyError) as e:
            messagebox.showerror("Error", str(e))
        self._refrescar()

    def _movimiento(self, tipo):
        codigo = self._seleccionado()
        if not codigo:
            return
        cantidad = simpledialog.askinteger(tipo.capitalize(), f"Cantidad a {tipo} de {codigo}:",
                                           minvalue=1, parent=self)
        if cantidad is None:
            return
        try:
            if tipo == "entrada":
                self.inv.registrar_entrada(codigo, cantidad, str(date.today()))
            else:
                self.inv.registrar_salida(codigo, cantidad, str(date.today()))
        except (ValueError, KeyError) as e:
            messagebox.showerror("Movimiento rechazado", str(e))
        self._refrescar()

    def _historial(self):
        codigo = self._seleccionado()
        if not codigo:
            return
        movs = self.inv.historial(codigo)
        texto = "\n".join(f"{m.fecha}  {m.tipo.upper():8}  {m.cantidad}" for m in movs) or "(sin movimientos)"
        messagebox.showinfo(f"Historial de {codigo}", texto)

    def _exportar(self, formato):
        ruta = RUTA_EXPORTACIONES / f"export_gui.{formato}"
        try:
            if formato == "json":
                guardar_productos_json(self.inv.todos(), ruta)
            else:
                guardar_productos_csv(self.inv.todos(), ruta)
        except IOError as e:
            messagebox.showerror("Error", str(e))
            return
        messagebox.showinfo("Exportado", f"Datos guardados en:\n{ruta}")


def lanzar():
    inv = Inventario(cargar_productos_json(RUTA_PRODUCTOS))
    app = InventarioGUI(inv)
    app.mainloop()


if __name__ == "__main__":
    lanzar()
