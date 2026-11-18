"use strict";

/* GUI web del sistema de inventario.
   Reutiliza los MISMOS módulos de lógica de negocio que la versión Node:
   ../src/models.js y ../src/inventario.js (formato UMD).
   Los datos se cargan por fetch desde ../../datos/productos.json; si se abre con
   file:// (fetch bloqueado), se usa la copia embebida SEED y se muestra un aviso. */

const { Inventario } = globalThis.InventarioMod;
const { Producto } = globalThis.Models;

// Copia intencional de datos/productos.json para modo archivo-local (file://).
const SEED = [
  { codigo: "P001", nombre: "Teclado USB", categoria: "Perifericos", precio: 25.50, existencia: 40, stock_minimo: 10 },
  { codigo: "P002", nombre: "Mouse Inalambrico", categoria: "Perifericos", precio: 15.75, existencia: 5, stock_minimo: 10 },
  { codigo: "P003", nombre: "Monitor 24 pulg", categoria: "Monitores", precio: 180.00, existencia: 12, stock_minimo: 4 },
  { codigo: "P004", nombre: "Laptop Pro 14", categoria: "Computo", precio: 950.00, existencia: 0, stock_minimo: 2 },
  { codigo: "P005", nombre: "Cable HDMI 2m", categoria: "Accesorios", precio: 8.90, existencia: 120, stock_minimo: 20 },
  { codigo: "P006", nombre: "Disco SSD 500GB", categoria: "Almacenamiento", precio: 65.00, existencia: 3, stock_minimo: 5 },
];

let inv;
let temporizadorMensaje;

// ---------- utilidades ----------
const $ = (sel) => document.querySelector(sel);

function mostrarError(e) {
  $("#mensaje").textContent = "⚠ " + e.message;
  $("#mensaje").className = "error";
  clearTimeout(temporizadorMensaje);
  temporizadorMensaje = setTimeout(() => { $("#mensaje").textContent = ""; $("#mensaje").className = ""; }, 4000);
}

function hoy() {
  const fecha = new Date();
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, "0")}-${String(fecha.getDate()).padStart(2, "0")}`;
}

function descargar(nombre, contenido, mime) {
  const blob = new Blob([contenido], { type: mime });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = nombre;
  a.hidden = true;
  document.body.appendChild(a);
  a.click();
  setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 1000);
}

// ---------- renderizado ----------
function productosFiltrados() {
  const reporte = $('input[name="reporte"]:checked').value;
  let productos;
  switch (reporte) {
    case "disponibles": productos = inv.disponibles(); break;
    case "agotados": productos = inv.agotados(); break;
    case "bajo": productos = inv.alertasBajoInventario(); break;
    case "mayor_valor": productos = inv.reporteMayorValor(); break;
    default: productos = inv.todos();
  }
  const texto = $("#buscar").value.trim().toLocaleLowerCase();
  const categoria = $("#categoria").value.trim().toLocaleLowerCase();
  return productos.filter((p) => p.nombre.toLocaleLowerCase().includes(texto)
    && p.categoria.toLocaleLowerCase().includes(categoria));
}

function refrescar() {
  let datos = productosFiltrados();
  const codigosVisibles = new Set(datos.map((p) => p.codigo));
  if (codigoSel && !codigosVisibles.has(codigoSel)) codigoSel = null;
  const orden = $("#orden").value;
  if (orden !== "codigo") {
    const clave = { precio: "precio", existencia: "existencia", categoria: "categoria" }[orden];
    datos = [...datos].sort((a, b) => a[clave] > b[clave] ? 1 : a[clave] < b[clave] ? -1 : 0);
  }
  const tbody = $("#tabla tbody");
  tbody.innerHTML = "";
  for (const p of datos) {
    const tr = document.createElement("tr");
    if (p.agotado) tr.classList.add("agotado");
    else if (p.bajoInventario) tr.classList.add("alerta");
    const valores = [p.codigo, p.nombre, p.categoria, `$${p.precio.toFixed(2)}`,
      String(p.existencia), String(p.stock_minimo), `$${p.valorTotal.toFixed(2)}`];
    for (const valor of valores) {
      const td = document.createElement("td");
      td.textContent = valor;
      tr.appendChild(td);
    }
    if (p.codigo === codigoSel) tr.classList.add("sel");
    tr.onclick = () => seleccionar(tr, p.codigo);
    tbody.appendChild(tr);
  }
  $("#total").textContent = `Valor total del inventario: $${inv.valorTotal().toLocaleString("en-US", { minimumFractionDigits: 2 })}`;
}

let codigoSel = null;
function seleccionar(tr, codigo) {
  document.querySelectorAll("#tabla tr").forEach((r) => r.classList.remove("sel"));
  tr.classList.add("sel");
  codigoSel = codigo;
}

function codigoElegido() {
  if (codigoSel) return codigoSel;
  const v = prompt("Código del producto:");
  return v ? v.trim() : null;
}

// ---------- acciones ----------
function accionNuevo() {
  const codigo = prompt("Código:"); if (codigo === null) return;
  const nombre = prompt("Nombre:"); if (nombre === null) return;
  const categoria = prompt("Categoría:"); if (categoria === null) return;
  const precioTexto = prompt("Precio:"); if (precioTexto === null) return;
  const existenciaTexto = prompt("Existencia:"); if (existenciaTexto === null) return;
  const minimoTexto = prompt("Stock mínimo:"); if (minimoTexto === null) return;
  const precio = Number(precioTexto);
  const existencia = Number(existenciaTexto);
  const stock_minimo = Number(minimoTexto);
  if (!Number.isFinite(precio) || !Number.isInteger(existencia) || !Number.isInteger(stock_minimo))
    return mostrarError(new Error("Precio debe ser numérico y existencia/mínimo deben ser enteros."));
  try {
    inv.agregarProducto(new Producto({ codigo, nombre, categoria, precio, existencia, stock_minimo }));
  } catch (e) { mostrarError(e); }
  refrescar();
}

function accionModificar() {
  const codigo = codigoElegido(); if (!codigo) return;
  const p = inv.consultarPorCodigo(codigo);
  if (!p) return mostrarError(new Error(`No existe producto con código ${codigo}.`));
  const nombre = prompt("Nombre:", p.nombre); if (nombre === null) return;
  const categoria = prompt("Categoría:", p.categoria); if (categoria === null) return;
  const precioTexto = prompt("Precio:", p.precio); if (precioTexto === null) return;
  const minimoTexto = prompt("Stock mínimo:", p.stock_minimo); if (minimoTexto === null) return;
  const precio = Number(precioTexto);
  const stock_minimo = Number(minimoTexto);
  if (!Number.isFinite(precio) || !Number.isInteger(stock_minimo))
    return mostrarError(new Error("Precio debe ser numérico y stock mínimo debe ser entero."));
  try {
    inv.modificarProducto(codigo, { nombre, categoria, precio, stock_minimo });
  } catch (e) { mostrarError(e); }
  refrescar();
}

function accionMovimiento(tipo) {
  const codigo = codigoElegido(); if (!codigo) return;
  const texto = prompt(`Cantidad a ${tipo} de ${codigo}:`);
  if (texto === null) return;
  const cantidad = Number(texto);
  if (!Number.isInteger(cantidad) || cantidad <= 0)
    return mostrarError(new Error("La cantidad debe ser un entero positivo."));
  try {
    if (tipo === "entrada") inv.registrarEntrada(codigo, cantidad, hoy());
    else inv.registrarSalida(codigo, cantidad, hoy());
  } catch (e) { mostrarError(e); }  // salida excesiva, producto inexistente, etc.
  refrescar();
}

function accionHistorial() {
  const codigo = codigoElegido(); if (!codigo) return;
  const movs = inv.historial(codigo);
  alert(movs.map((m) => `${m.fecha}  ${m.tipo.toUpperCase()}  ${m.cantidad}`).join("\n") || "(sin movimientos)");
}

function accionExportar(formato) {
  const datos = inv.todos().map((p) => p.toDict());
  if (formato === "json") {
    descargar("productos_exportados.json", JSON.stringify(datos, null, 2), "application/json");
  } else {
    const enc = "codigo,nombre,categoria,precio,existencia,stock_minimo";
    const celda = (valor) => `"${String(valor).replaceAll('"', '""')}"`;
    const filas = datos.map((p) => [p.codigo, p.nombre, p.categoria, p.precio, p.existencia, p.stock_minimo]
      .map(celda).join(","));
    descargar("productos_exportados.csv", [enc, ...filas].join("\n"), "text/csv");
  }
}

// ---------- arranque ----------
async function iniciar() {
  let datos = SEED;
  try {
    const resp = await fetch("../../datos/productos.json");
    if (resp.ok) datos = await resp.json();
    else throw new Error("HTTP " + resp.status);
  } catch (e) {
    $("#mensaje").textContent = "Modo sin servidor: usando datos embebidos (los cambios no se guardan en disco).";
    $("#mensaje").className = "aviso";
  }
  inv = new Inventario(datos.map((p) => new Producto(p)));
  document.querySelectorAll("#acciones button").forEach((b) => {
    b.onclick = {
      nuevo: accionNuevo,
      modificar: accionModificar,
      entrada: () => accionMovimiento("entrada"),
      salida: () => accionMovimiento("salida"),
      historial: accionHistorial,
      json: () => accionExportar("json"),
      csv: () => accionExportar("csv"),
    }[b.dataset.acc];
  });
  ["#buscar", "#categoria"].forEach((s) => $(s).addEventListener("input", refrescar));
  $("#orden").addEventListener("change", refrescar);
  document.querySelectorAll('input[name="reporte"]').forEach((r) => r.addEventListener("change", refrescar));
  refrescar();
}

document.addEventListener("DOMContentLoaded", iniciar);
