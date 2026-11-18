"use strict";

const fs = require("fs");
const { Producto, Movimiento } = require("./models");

/** Carga y guardado de datos en JSON y CSV (JavaScript, solo Node estándar). */
function cargarProductosJson(ruta) {
  try {
    const datos = JSON.parse(fs.readFileSync(ruta, "utf-8"));
    return datos.map((p) => new Producto(p));
  } catch (e) {
    throw new Error(`No se pudieron cargar los productos desde ${ruta}: ${e.message}`);
  }
}

function guardarProductosJson(productos, ruta) {
  fs.writeFileSync(ruta, JSON.stringify(productos.map((p) => p.toDict()), null, 2));
}

function cargarProductosCsv(ruta) {
  try {
    const lineas = fs.readFileSync(ruta, "utf-8").split(/\r?\n/).filter((l) => l.trim());
    lineas.shift(); // encabezado
    return lineas.map((l) => {
      const [codigo, nombre, categoria, precio, existencia, stock_minimo] = l.split(",");
      return new Producto({ codigo, nombre, categoria, precio, existencia, stock_minimo });
    });
  } catch (e) {
    throw new Error(`No se pudieron cargar los productos desde ${ruta}: ${e.message}`);
  }
}

function guardarProductosCsv(productos, ruta) {
  const encabezado = "codigo,nombre,categoria,precio,existencia,stock_minimo";
  const filas = productos.map((p) =>
    `${p.codigo},${p.nombre},${p.categoria},${p.precio},${p.existencia},${p.stock_minimo}`);
  fs.writeFileSync(ruta, [encabezado, ...filas].join("\n"));
}

function cargarMovimientosJson(ruta) {
  try {
    return JSON.parse(fs.readFileSync(ruta, "utf-8")).map((m) => new Movimiento(m));
  } catch (e) {
    throw new Error(`No se pudieron cargar los movimientos desde ${ruta}: ${e.message}`);
  }
}

function guardarMovimientosJson(movimientos, ruta) {
  fs.writeFileSync(ruta, JSON.stringify(movimientos.map((m) => m.toDict()), null, 2));
}

module.exports = {
  cargarProductosJson, guardarProductosJson,
  cargarProductosCsv, guardarProductosCsv,
  cargarMovimientosJson, guardarMovimientosJson,
};
