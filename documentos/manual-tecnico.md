# Manual técnico — Sistema de inventario Policode

## 1. Propósito y requisitos

Este manual describe cómo instalar, ejecutar, probar y extender las tres implementaciones del inventario. Requisitos mínimos: Python 3.10+, JDK 17+ y Node.js 18+. No hay dependencias externas obligatorias. En Windows se ejecutan los comandos desde PowerShell y se usa `javac -encoding UTF-8` para conservar caracteres en español.

## 2. Organización del repositorio

- `datos/`: dataset completo (`productos.json`, `productos.csv`, `movimientos.json`) y tres fixtures por tamaño. El pequeño tiene dos productos; el mediano cuatro; el grande seis.
- `python/src/`: `models.py` define Producto y Movimiento, `inventario.py` contiene reglas y reportes, `persistencia.py` implementa JSON/CSV, `main.py` es CLI y `gui.py` es tkinter.
- `java/src/`: `Producto.java` y `Movimiento.java` modelan entidades, `Inventario.java` implementa reglas, `Persistencia.java` lee/escribe archivos, `Main.java` es CLI y `InventarioGUI.java` es Swing.
- `javascript/src/`: `models.js`, `inventario.js`, `persistencia.js` y `main.js`. `javascript/gui/` contiene la presentación web y carga la misma lógica de inventario.
- `*/tests/`: casos automatizados de negocio, validación, rollback, reportes y persistencia.
- `documentos/diagramas/`: fuentes Mermaid y versiones SVG de la arquitectura y el lote transaccional.

## 3. Modelo de datos

Un producto tiene `codigo` (cadena única), `nombre`, `categoria`, `precio` (número), `existencia` (entero no negativo) y `stock_minimo` (entero no negativo). Un movimiento contiene `codigo_producto`, `tipo` (`entrada` o `salida`), `cantidad` (entero positivo) y `fecha`.

El JSON contiene una lista de objetos de producto. El CSV contiene el mismo esquema como columnas. Los fixtures por tamaño son subconjuntos del catálogo completo y se usan para experimentar con volúmenes distintos; los resultados de referencia documentados corresponden a los seis productos de `productos.json`.

## 4. Reglas de negocio

1. Rechazar códigos duplicados y campos requeridos vacíos.
2. Rechazar valores monetarios, existencias o mínimos negativos.
3. Aceptar solo entradas/salidas con cantidad entera mayor que cero.
4. Rechazar una salida que exceda la existencia y no modificar el estado.
5. Considerar agotado el producto con existencia cero. Clasificar como bajo inventario cuando la existencia sea menor o igual al mínimo, pero excluir agotados de la lista de alertas.
6. Calcular el valor de cada artículo como precio por existencia y el valor global como la suma, redondeada a dos decimales.
7. Procesar lotes de forma atómica. Ante error, restaurar existencias y borrar movimientos registrados durante ese lote.

## 5. Ejecución y pruebas

### Python

```powershell
cd python
python -m unittest discover -s tests -v
$env:PYTHONPATH = "src"; python src/main.py
```

Para la GUI, compilar no aplica: entra a `python/src` y ejecuta `python gui.py`.

### Java

```powershell
cd java/src
javac -encoding UTF-8 *.java
java Main
cd ../tests
javac -encoding UTF-8 -cp ../src -d . InventarioTest.java
java -cp "../src;." InventarioTest
```

Para Swing, desde `java/src` ejecuta `java InventarioGUI` después de compilar.

### JavaScript

```powershell
cd javascript
npm test
npm start
```

La interfaz web puede abrirse sirviendo `javascript/gui/` con `python -m http.server 8080`.

## 6. Interfaz de negocio y resultados

Las operaciones principales son agregar/modificar/consultar/buscar producto, registrar entrada/salida, procesar lote, ver historial, ordenar, consultar alertas y generar reportes. Las tres clases/módulos `Inventario` mantienen los nombres equivalentes aunque respetan las convenciones de cada lenguaje.

Para el dataset completo se esperan: valor total $4,521.75; producto agotado P004; alertas P002 y P006; P003 con mayor valor almacenado ($2,160.00). El README raíz contiene el resumen de reglas y la ruta de cada interfaz.

## 7. Errores y extensiones

Los errores de validación se expresan como excepciones. Python usa ValueError/KeyError/IOError; Java IllegalArgumentException/NoSuchElementException/IOException; JavaScript Error. Las pruebas cubren como mínimo cinco escenarios comunes en los tres lenguajes; el conteo actual es Python 18, Java 22 y JavaScript 12.

El almacenamiento es archivo local, no una base de datos concurrente. El CSV Java implementa un formato sencillo y no pretende cubrir todos los casos de comillas/comas en campos. Para producción se recomienda usar un parser CSV estándar, dinero en centavos enteros/decimal fijo, persistencia transaccional y control de acceso. El equipo debe completar portada y evidencias reales de las GUIs.
