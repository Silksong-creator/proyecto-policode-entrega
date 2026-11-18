"use strict";

// UMD: depende de ./models en Node o de window.Models en el navegador.
// Se accede como _deps.Producto / _deps.Movimiento para no redeclarar los
// identificadores globales que ya expone models.js en el navegador.
// Nota: NO se declaran 'Producto'/'Movimiento' a nivel superior para no
// colisionar con los identificadores globales que expone models.js en el navegador.
const _deps =
  typeof module === "object" && module.exports ? require("./models") : globalThis.Models;
const _P = _deps.Producto;
const _M = _deps.Movimiento;

/** Lógica de negocio del sistema de inventario (JavaScript). */
class Inventario {
  constructor(productos = []) {
    this._productos = new Map();   // Map preserva el orden de inserción
    productos.forEach((p) => this.agregarProducto(p));
    this._movimientos = [];
  }

  // ---------- Productos ----------
  agregarProducto(producto) {
    if (!(producto instanceof _P))
      producto = new _P(producto);
    if (this._productos.has(producto.codigo))
      throw new Error(`Ya existe un producto con código ${producto.codigo}.`);
    this._productos.set(producto.codigo, producto);
  }

  /** Modifica los campos permitidos de un producto existente. */
  modificarProducto(codigo, cambios = {}) {
    const p = this._obtener(codigo);
    const permitidos = ["nombre", "categoria", "precio", "stock_minimo"];
    const entradas = Object.entries(cambios);
    for (const [campo, valor] of entradas) {
      if (!permitidos.includes(campo))
        throw new Error(`Campo no modificable: ${campo}.`);
      if (campo === "precio" && (!Number.isFinite(Number(valor)) || Number(valor) < 0))
        throw new Error("El precio debe ser un número no negativo.");
      if (campo === "stock_minimo" && (!Number.isInteger(Number(valor)) || Number(valor) < 0))
        throw new Error("El stock mínimo debe ser un entero no negativo.");
      if (["nombre", "categoria"].includes(campo) && !String(valor).trim())
        throw new Error(`El campo ${campo} es obligatorio.`);
    }
    for (const [campo, valor] of entradas)
      p[campo] = ["precio", "stock_minimo"].includes(campo) ? Number(valor) : String(valor).trim();
    return p;
  }

  /** Devuelve el producto o undefined si no existe. */
  consultarPorCodigo(codigo) { return this._productos.get(codigo); }

  /** Busca por nombre (parcial) y/o categoría: filter = estilo funcional. */
  buscar(texto = "", categoria = "") {
    const t = texto.toLowerCase(), c = categoria.toLowerCase();
    return [...this._productos.values()].filter(
      (p) => p.nombre.toLowerCase().includes(t) && p.categoria.toLowerCase().includes(c));
  }

  todos() { return [...this._productos.values()]; }

  // ---------- Movimientos ----------
  registrarEntrada(codigo, cantidad, fecha) {
    const p = this._obtener(codigo);
    const movimiento = new _M({ codigo_producto: codigo, tipo: "entrada", cantidad, fecha });
    p.existencia += cantidad;
    this._movimientos.push(movimiento);
  }

  registrarSalida(codigo, cantidad, fecha) {
    const p = this._obtener(codigo);
    // Regla de negocio: nunca permitir salidas superiores al inventario
    if (cantidad > p.existencia)
      throw new Error(
        `Salida inválida: se solicitaron ${cantidad} unidades de ${p.nombre} pero solo hay ${p.existencia} disponibles.`);
    const movimiento = new _M({ codigo_producto: codigo, tipo: "salida", cantidad, fecha });
    p.existencia -= cantidad;
    this._movimientos.push(movimiento);
  }

  /** Procesa varios movimientos; si uno falla, no quedan cambios parciales. */
  procesarLote(lote) {
    const copia = new Map([...this._productos].map(([k, p]) => [k, p.existencia]));
    const marcador = this._movimientos.length;
    try {
      for (const m of lote) {
        if (m.tipo === "entrada") this.registrarEntrada(m.codigo_producto, m.cantidad, m.fecha);
        else if (m.tipo === "salida") this.registrarSalida(m.codigo_producto, m.cantidad, m.fecha);
        else throw new Error(`Tipo desconocido: ${m.tipo}`);
      }
    } catch (e) {
      for (const [k, e0] of copia) this._productos.get(k).existencia = e0;
      this._movimientos.length = marcador;
      throw e;
    }
    return lote;
  }

  /** Historial de movimientos, opcionalmente filtrado por producto. */
  historial(codigo = null) {
    if (codigo === null) return [...this._movimientos];
    return this._movimientos.filter((m) => m.codigo_producto === codigo);
  }

  // ---------- Reglas de negocio y reportes ----------
  valorTotal() {
    const total = this.todos().reduce((acc, p) => acc + p.valorTotal, 0);
    return Math.round(total * 100) / 100;
  }

  alertasBajoInventario() {
    return this.todos().filter((p) => p.bajoInventario && !p.agotado);
  }

  agotados() { return this.todos().filter((p) => p.agotado); }
  disponibles() { return this.todos().filter((p) => !p.agotado); }

  /** Ordena por precio, existencia o categoria: sort con comparador por criterio. */
  ordenar(criterio = "precio", inverso = false) {
    const comparadores = {
      precio: (a, b) => a.precio - b.precio,
      existencia: (a, b) => a.existencia - b.existencia,
      categoria: (a, b) => a.categoria.localeCompare(b.categoria),
    };
    if (!comparadores[criterio])
      throw new Error("Criterio inválido. Use 'precio', 'existencia' o 'categoria'.");
    const ordenados = [...this.todos()].sort(comparadores[criterio]);
    return inverso ? ordenados.reverse() : ordenados;
  }

  reporteMayorValor(limite = null) {
    const ordenados = [...this.todos()].sort((a, b) => b.valorTotal - a.valorTotal);
    return limite === null ? ordenados : ordenados.slice(0, limite);
  }

  _obtener(codigo) {
    const p = this._productos.get(codigo);
    if (!p) throw new Error(`No existe producto con código ${codigo}.`);
    return p;
  }
}

if (typeof module === "object" && module.exports) {
  module.exports = { Inventario };
} else {
  globalThis.InventarioMod = { Inventario };
}
