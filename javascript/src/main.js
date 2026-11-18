"use strict";

const { Inventario } = require("./inventario");
const { Producto } = require("./models");
const { cargarProductosJson, guardarProductosJson } = require("./persistencia");

const RUTA_PRODUCTOS = "../datos/productos.json";
const RUTA_SALIDA = "../datos/productos_exportados_js.json";

function mostrar(titulo, lista) {
  console.log(`\n== ${titulo} ==`);
  if (lista.length === 0) console.log("  (sin resultados)");
  for (const p of lista)
    console.log(`  ${p.codigo} | ${p.nombre.padEnd(20)} | ${p.categoria.padEnd(14)} | ` +
      `$${p.precio.toFixed(2).padStart(8)} | exist: ${String(p.existencia).padStart(4)} | ` +
      `valor: $${p.valorTotal.toFixed(2).padStart(10)}`);
}

function demo() {
  const inv = new Inventario(cargarProductosJson(RUTA_PRODUCTOS));
  mostrar("Todos los productos", inv.todos());
  mostrar("Disponibles", inv.disponibles());
  mostrar("Agotados", inv.agotados());
  mostrar("Bajo inventario (ALERTA)", inv.alertasBajoInventario());
  mostrar("Mayor valor", inv.reporteMayorValor(3));
  console.log(`\nValor total del inventario: $${inv.valorTotal().toFixed(2)}`);

  inv.procesarLote([
    { codigo_producto: "P001", tipo: "entrada", cantidad: 10, fecha: "2026-11-10" },
    { codigo_producto: "P002", tipo: "salida", cantidad: 2, fecha: "2026-11-10" },
  ]);
  console.log("Historial P001:", inv.historial("P001"));
  guardarProductosJson(inv.todos(), RUTA_SALIDA);
  console.log("Datos exportados a productos_exportados_js.json");
}

if (require.main === module) demo();
module.exports = { demo };
