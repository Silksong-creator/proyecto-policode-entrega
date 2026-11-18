"use strict";

const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const os = require("os");
const path = require("path");

const { Inventario } = require("../src/inventario");
const { Producto, Movimiento } = require("../src/models");
const { cargarProductosJson, guardarProductosJson,
        cargarProductosCsv, guardarProductosCsv } = require("../src/persistencia");

const demo = () => new Inventario([
  new Producto({ codigo: "P001", nombre: "Teclado USB", categoria: "Perifericos", precio: 25.50, existencia: 40, stock_minimo: 10 }),
  new Producto({ codigo: "P002", nombre: "Mouse Inalambrico", categoria: "Perifericos", precio: 15.75, existencia: 5, stock_minimo: 10 }),
  new Producto({ codigo: "P004", nombre: "Laptop Pro 14", categoria: "Computo", precio: 950.00, existencia: 0, stock_minimo: 2 }),
]);

test("registrar y consultar producto por código", () => {
  const inv = demo();
  assert.equal(inv.consultarPorCodigo("P001").nombre, "Teclado USB");
  assert.equal(inv.consultarPorCodigo("ZZZ"), undefined);
});

test("no permite código duplicado", () => {
  const inv = demo();
  assert.throws(() => inv.agregarProducto(new Producto(
    { codigo: "P001", nombre: "Otro", categoria: "X", precio: 1, existencia: 1, stock_minimo: 1 })), /Ya existe/);
});

test("modificar producto existente y rechazar inexistente", () => {
  const inv = demo();
  const p = inv.modificarProducto("P001", { precio: 30.0, nombre: "Teclado Mecanico" });
  assert.equal(p.precio, 30.0);
  assert.equal(p.nombre, "Teclado Mecanico");
  assert.throws(() => inv.modificarProducto("P001", { nombre: "Cambio parcial", precio: Infinity }), /número no negativo/);
  assert.equal(p.nombre, "Teclado Mecanico");
  assert.throws(() => inv.modificarProducto("ZZZ", { precio: 1 }), /No existe/);
});

test("buscar por nombre y categoría", () => {
  const inv = demo();
  assert.equal(inv.buscar("teclado").length, 1);
  assert.equal(inv.buscar("", "perifericos").length, 2);
  assert.equal(inv.buscar("mouse", "perifericos").length, 1);
});

test("validación de datos inválidos", () => {
  assert.throws(() => new Producto({ codigo: "", nombre: "X", categoria: "X", precio: 1, existencia: 1, stock_minimo: 1 }), /obligatorio/);
  assert.throws(() => new Producto({ codigo: "P1", nombre: "X", categoria: "X", precio: -5, existencia: 1, stock_minimo: 1 }), /negativo/);
  assert.throws(() => new Producto({ codigo: "P1", nombre: "X", categoria: "X", precio: NaN, existencia: 1, stock_minimo: 1 }), /número/);
  assert.throws(() => new Producto({ codigo: "P1", nombre: "X", categoria: "X", precio: 1, existencia: 1.5, stock_minimo: 1 }), /entero/);
  assert.throws(() => new Movimiento({ codigo_producto: "P1", tipo: "ajuste", cantidad: 3, fecha: "2026-01-01" }), /inválido/);
});

test("entrada actualiza existencia y registra historial", () => {
  const inv = demo();
  assert.throws(() => inv.registrarEntrada("P001", 0, "2026-11-10"), /entero positivo/);
  assert.equal(inv.consultarPorCodigo("P001").existencia, 40);
  inv.registrarEntrada("P001", 10, "2026-11-10");
  assert.equal(inv.consultarPorCodigo("P001").existencia, 50);
  assert.equal(inv.historial().length, 1);
});

test("salida mayor a la existencia es rechazada sin cambios", () => {
  const inv = demo();
  assert.throws(() => inv.registrarSalida("P002", 99, "2026-11-10"), /Salida inválida/);
  assert.equal(inv.consultarPorCodigo("P002").existencia, 5);
});

test("procesar lote válido y con rollback ante fallo", () => {
  const inv = demo();
  inv.procesarLote([
    { codigo_producto: "P001", tipo: "entrada", cantidad: 5, fecha: "2026-11-10" },
    { codigo_producto: "P001", tipo: "salida", cantidad: 3, fecha: "2026-11-11" },
  ]);
  assert.equal(inv.consultarPorCodigo("P001").existencia, 42);
  assert.equal(inv.historial("P001").length, 2);

  const inv2 = demo();
  assert.throws(() => inv2.procesarLote([
    { codigo_producto: "P001", tipo: "entrada", cantidad: 5, fecha: "2026-11-10" },
    { codigo_producto: "P002", tipo: "salida", cantidad: 99, fecha: "2026-11-10" },
  ]), /Salida inválida/);
  assert.equal(inv2.consultarPorCodigo("P001").existencia, 40);
  assert.equal(inv2.historial().length, 0);
});

test("valor total, alertas, agotados y disponibles", () => {
  const inv = demo();
  assert.equal(inv.valorTotal(), Math.round((25.50 * 40 + 15.75 * 5) * 100) / 100);
  assert.deepEqual(inv.alertasBajoInventario().map((p) => p.codigo), ["P002"]);
  assert.deepEqual(inv.agotados().map((p) => p.codigo), ["P004"]);
  assert.equal(inv.disponibles().length, 2);
});

test("ordenar por criterios y reporte de mayor valor", () => {
  const inv = demo();
  assert.deepEqual(inv.ordenar("precio").map((p) => p.codigo), ["P002", "P001", "P004"]);
  assert.equal(inv.ordenar("existencia", true)[0].codigo, "P001");
  assert.equal(inv.reporteMayorValor(1)[0].codigo, "P001");
  assert.throws(() => inv.ordenar("peso"), /Criterio inválido/);
});

test("persistencia JSON y CSV (redondo)", () => {
  const inv = demo();
  const carpeta = fs.mkdtempSync(path.join(os.tmpdir(), "inventario-js-"));
  const json = path.join(carpeta, "productos.json");
  const csv = path.join(carpeta, "productos.csv");
  try {
    guardarProductosJson(inv.todos(), json);
    guardarProductosCsv(inv.todos(), csv);
    assert.deepEqual(cargarProductosJson(json).map((p) => p.toDict()),
                     cargarProductosCsv(csv).map((p) => p.toDict()));
  } finally {
    fs.rmSync(carpeta, { recursive: true, force: true });
  }
});

test("cargar archivo inexistente lanza error", () => {
  assert.throws(() => cargarProductosJson(path.join(os.tmpdir(), "inventario-no-existe.json")), /No se pudieron cargar/);
});
