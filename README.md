# Sistema de Control de Inventario Policode

Tres versiones equivalentes del sistema: Python, Java y JavaScript. Cada una modela productos y movimientos, aplica las mismas reglas de negocio y ofrece búsquedas, alertas, reportes y persistencia JSON/CSV. La carpeta `datos/` es el dataset común.

## Estructura

```text
inventario-policode/
├── README.md
├── datos/
│   ├── productos_pequenos.json
│   ├── productos_medianos.json
│   ├── productos_grandes.json
│   ├── productos.json
│   ├── productos.csv
│   └── movimientos.json
├── python/
│   ├── src/                # modelos, negocio, persistencia, CLI y GUI
│   ├── tests/
│   └── requirements.txt
├── java/
│   ├── src/                # modelos, negocio, persistencia, CLI y Swing
│   ├── tests/
│   └── README-EJECUCION.md
├── javascript/
│   ├── src/                # modelos, negocio, persistencia y CLI
│   ├── tests/
│   ├── gui/
│   └── package.json
├── documentos/
│   ├── analisis-comparativo.pdf
│   ├── manual-tecnico.pdf
│   ├── analisis-comparativo.md
│   ├── manual-tecnico.md
│   └── diagramas/           # arquitectura y flujo transaccional (SVG/Mermaid)
└── evidencias/
    ├── capturas/
    └── videos/
```

## Reglas comunes

- Código de producto único; código y nombre obligatorios.
- Precio, existencia y mínimo no negativos; movimiento de entrada o salida con cantidad entera positiva.
- Una salida no puede exceder las existencias.
- Agotado significa existencia cero. Bajo inventario significa existencia menor o igual al mínimo, sin incluir agotados en la alerta.
- Valor total = suma de precio por existencia, redondeada a dos decimales.
- Los lotes son transaccionales: un error revierte existencias e historial del lote.
- Los informes ofrecen disponibles, agotados, alertas, orden y mayor valor.

## Requisitos

- Python 3.10 o superior (biblioteca estándar).
- JDK 17 o superior.
- Node.js 18 o superior.

## Ejecutar y probar en Windows PowerShell

### Python

```powershell
cd python
python -m unittest discover -s tests -v
$env:PYTHONPATH = "src"; python src/main.py
```

Para iniciar la GUI: desde `python/src`, ejecuta `python gui.py`.
También puedes abrirla desde la raíz con `python python/src/gui.py`; la ruta al catálogo ya no depende del directorio actual.

### Java

```powershell
cd java/src
javac -encoding UTF-8 *.java
java Main
cd ../tests
javac -encoding UTF-8 -cp ../src -d . InventarioTest.java
java -cp "../src;." InventarioTest
```

La GUI Swing se inicia desde `java/src` con `java InventarioGUI` después de compilar. `RutaDatos` encuentra el dataset aunque inicies Java desde la raíz del proyecto.

### JavaScript

```powershell
cd javascript
npm test
npm start
```

Para abrir la GUI web, inicia el servidor desde la raíz del proyecto con `python -m http.server 8080` y visita `http://localhost:8080/javascript/gui/`. Así la GUI descarga el mismo `datos/productos.json` que las otras versiones.

## Resultados de referencia

Para el dataset inicial: valor total **$4,521.75**; P004 agotado; P002 y P006 bajo inventario; P003 con mayor valor almacenado (**$2,160.00**).

Revisión automatizada: Python **18/18**, Java **22/22**, JavaScript **12/12**. Ejecuta los comandos anteriores para reproducirlos.

## Git y evidencias

El repositorio Git ya está inicializado y contiene el estado entregable actual. Para continuar registrando cambios:

```powershell
git add -A
git commit -m "Describe el cambio"
```

Los tres JSON por tamaño son subconjuntos del mismo catálogo: pequeño (2 productos), mediano (4) y grande (6). Cada implementación puede apuntar al archivo elegido; los resultados esperados del README corresponden al conjunto completo `productos.json`. La carpeta `evidencias/` debe completarse con capturas y/o video generados por el equipo al ejecutar cada GUI. No se agregaron evidencias simuladas. Agrega nombres de integrantes, institución, grupo y docente a la portada antes de entregar.

El código no contiene claves ni contraseñas. El análisis comparativo está en `documentos/analisis-comparativo.md` y `documentos/analisis-comparativo.pdf`; la guía detallada está en `documentos/manual-tecnico.pdf`.
