"use strict";

/** Representa un producto del inventario (JavaScript). */
class Producto {
  constructor({ codigo, nombre, categoria, precio, existencia, stock_minimo }) {
    if (!codigo || !String(codigo).trim()) throw new Error("El código del producto es obligatorio.");
    if (!nombre || !String(nombre).trim()) throw new Error("El nombre del producto es obligatorio.");
    precio = Number(precio);
    existencia = Number(existencia);
    stock_minimo = Number(stock_minimo);
    if (!Number.isFinite(precio) || precio < 0) throw new Error("El precio debe ser un número no negativo.");
    if (!Number.isInteger(existencia) || existencia < 0) throw new Error("La existencia debe ser un entero no negativo.");
    if (!Number.isInteger(stock_minimo) || stock_minimo < 0) throw new Error("El stock mínimo debe ser un entero no negativo.");
    this.codigo = String(codigo).trim();
    this.nombre = String(nombre).trim();
    this.categoria = String(categoria).trim();
    this.precio = Number(precio);
    this.existencia = Number(existencia);
    this.stock_minimo = Number(stock_minimo);
  }

  /** Valor monetario almacenado de este producto. */
  get valorTotal() { return Math.round(this.precio * this.existencia * 100) / 100; }
  get agotado() { return this.existencia === 0; }
  /** Alerta: existencia menor o igual al stock mínimo. */
  get bajoInventario() { return this.existencia <= this.stock_minimo; }

  toDict() {
    return {
      codigo: this.codigo, nombre: this.nombre, categoria: this.categoria,
      precio: this.precio, existencia: this.existencia, stock_minimo: this.stock_minimo,
    };
  }
}

/** Registro de una entrada o salida de mercancía (JavaScript). */
class Movimiento {
  static TIPOS_VALIDOS = ["entrada", "salida"];

  constructor({ codigo_producto, tipo, cantidad, fecha }) {
    if (!Movimiento.TIPOS_VALIDOS.includes(tipo))
      throw new Error(`Tipo de movimiento inválido: ${tipo}. Use 'entrada' o 'salida'.`);
    if (!Number.isInteger(cantidad) || cantidad <= 0)
      throw new Error("La cantidad debe ser un entero positivo.");
    this.codigo_producto = codigo_producto;
    this.tipo = tipo;
    this.cantidad = cantidad;
    this.fecha = fecha;
  }

  toDict() {
    return {
      codigo_producto: this.codigo_producto, tipo: this.tipo,
      cantidad: this.cantidad, fecha: this.fecha,
    };
  }
}

// UMD: funciona en Node (CommonJS) y en el navegador (window)
if (typeof module === "object" && module.exports) {
  module.exports = { Producto, Movimiento };
} else {
  globalThis.Models = { Producto, Movimiento };
}
