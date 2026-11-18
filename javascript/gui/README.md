# GUI web (JavaScript)

Desde la raíz del repositorio inicia el servidor local y abre la ruta de la GUI. Así el navegador puede cargar el dataset compartido:

```powershell
python -m http.server 8080
```

Abre `http://localhost:8080/javascript/gui/`. El servidor debe arrancarse desde la raíz del repositorio; iniciado dentro de `javascript/gui`, los archivos de lógica y `datos/productos.json` quedan fuera de la raíz servida.

Si abres `index.html` directamente con `file://`, fetch queda bloqueado y la interfaz usa los datos iniciales embebidos. En ese modo las exportaciones descargan archivos, no guardan cambios en el catálogo del proyecto.

La GUI reutiliza **los mismos** `src/models.js` y `src/inventario.js` que la
versión Node (formato UMD), por lo que las pruebas automatizadas de `tests/`
cubren exactamente el código que usa la interfaz.
