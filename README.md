# Sistema de Control de Inventario Policode

Tres versiones equivalentes del sistema: Python, Java y JavaScript. Cada una modela productos y movimientos, aplica las mismas reglas de negocio y ofrece búsquedas, alertas, reportes y persistencia JSON/CSV. La carpeta `datos/` es el dataset común.

## Equipo 4

| Nombre completo | Nickname |
|---|---|
| Dario Ortega Sosa | `-Silksong-creator` |
| Estrella Gomez Rebolledo | `EstrellaGoRe` |
| Yamelin Perez Barrera | `yamelin-lol` |
| Eric Navarro Lopez | `Eric10yt` |

## Estructura

```text
inventario-policode/
├── README.md
├── iniciar.bat            # iniciador de Windows
├── iniciar.ps1             # instala runtimes, prepara y abre las aplicaciones
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
│   ├── manual-usuario.pdf
│   ├── manual-tecnico.pdf
│   ├── analisis-comparativo.md
│   ├── manual-usuario.md
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
- Las tres interfaces muestran una ventana emergente cuando inicia la aplicación con productos bajo el mínimo y cuando una operación válida hace que un producto nuevo llegue a ese nivel; el aviso no se repite mientras el mismo artículo siga bajo. También queda visible una banda persistente en la interfaz.

## Requisitos

- Python 3.10 o superior (biblioteca estándar).
- JDK 17 o superior.
- Node.js 18 o superior.

## Iniciar con `iniciar.bat` en Windows

1. Descomprime el proyecto completo y abre la carpeta `inventario-policode`.
2. Haz doble clic en **`iniciar.bat`**. También puedes abrir PowerShell en esa carpeta y ejecutar `.\iniciar.bat`.
3. El iniciador comprueba Python 3.10 o posterior, JDK 17 o posterior y Node.js 18 o posterior. Si falta alguno, intenta instalarlo con **Winget**. Acepta los avisos del instalador y espera a que termine.
4. Si se instaló algún runtime, cierra la ventana, abre de nuevo `iniciar.bat` y espera la preparación inicial. El iniciador instala los paquetes declarados, compila Java en `java/build/` y muestra el menú.
5. Elige una opción del menú:

   - `1`: abre Python/Tkinter.
   - `2`: abre Java/Swing.
   - `3`: inicia el servidor local y abre JavaScript/Web en el navegador. Deja la consola abierta; presiona `Ctrl+C` en ella para detener el servidor.
   - `4`: ejecuta las pruebas de Python, Java y JavaScript.
   - `5`: vuelve a preparar las dependencias y compila Java.
   - `0`: cierra el menú.

El proyecto no tiene paquetes externos declarados actualmente: usa la biblioteca estándar de Python, las APIs incluidas en el JDK y Node.js. Si Winget no está instalado o no puede instalar un runtime, el iniciador muestra los sitios oficiales para instalarlo manualmente. `iniciar.ps1` contiene las acciones; `iniciar.bat` lo abre con la política de ejecución limitada a ese proceso.

## Ejecutar y probar manualmente en Windows PowerShell

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
