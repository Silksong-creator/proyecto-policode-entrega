# Análisis comparativo: Sistema de Control de Inventario en Python, Java y JavaScript

**Portada.** Sistema de Control de Inventario — Proyecto Policode. Implementaciones
funcionalmente equivalentes en Python (tkinter), Java (Swing) y JavaScript (web + Node).
Fecha: noviembre de 2026.

## 1. Introducción
Este trabajo implementa el mismo sistema de inventario en tres lenguajes para comparar
cómo cada uno modela datos, procesa reglas de negocio y maneja errores. La equivalencia
funcional se garantiza compartiendo el mismo dataset (`datos/productos.json`) y las mismas
reglas: salidas nunca superiores a la existencia, alerta cuando existencia ≤ stock mínimo,
valor total = Σ(precio × existencia) y procesamiento por lotes transaccional.

## 2. Descripción del problema
Empresa comercial que necesita controlar entradas, salidas y existencias. Dificultades del
sistema actual: identificar productos con bajo inventario, evitar salidas superiores a la
existencia y conocer el valor total almacenado.

## 3. Reglas de negocio (idénticas en las tres versiones)
1. Producto: `codigo` (único), `nombre`, `categoria`, `precio`, `existencia`, `stock_minimo`.
2. Código y nombre obligatorios; precio y existencia ≥ 0.
3. Movimiento: tipo `entrada` o `salida`; cantidad entera positiva.
4. **Una salida nunca supera la existencia disponible**; se rechaza con error y sin cambios.
5. **Alerta de bajo inventario:** existencia ≤ stock_minimo y no agotado. Agotado = 0.
6. Valor total = Σ (precio × existencia).
7. Lotes transaccionales: si un movimiento falla, no quedan cambios parciales.
8. Reportes: disponibles, agotados, bajo inventario, mayor valor; orden por precio/existencia/categoría.

## 4. Justificación de los lenguajes
- **Python**: productividad y legibilidad; ideal para demostrar OOP + funcional con mínimo código.
- **Java**: tipado estático, portabilidad JVM y robustez; estándar en sistemas empresariales.
- **JavaScript/Node**: mismo lenguaje en navegador y servidor; asincronía nativa.

## 5. Clasificación de cada lenguaje

| Criterio | Python | Java | JavaScript |
|---|---|---|---|
| a. Nivel de abstracción | Alto | Alto | Alto |
| b. Ejecución | Interpretado (bytecode en la VM de CPython) | Compilado a bytecode, ejecutado en JVM | Interpretado/JIT (V8) |
| c. Sistema de tipos | Dinámico y fuerte (gradual con anotaciones) | Estático y fuerte (con autoboxing) | Dinámico y débil (TS lo vuelve estático) |
| d. Propósito | General, datos, automatización | Sistemas y servicios | Web cliente/servidor, automatización |
| e. Paradigmas | Multiparadigma: OO + funcional (lambda, comprensiones) | Principal: OO; funcional con Streams/lambdas | Multiparadigma: prototipos + funcional de 1.ª clase + eventos |
| f. Tipo de lenguaje | Imperativo, OO, funcional parcial, textual | Imperativo, OO, funcional, concurrente (hilos) | Imperativo, OO, funcional, **orientado a eventos** |

*Nota (c):* «fuerte/débil» no es binario. Python es dinámico pero fuerte (`"1"+1` lanza
`TypeError`); JavaScript es débil (`1+"1"` = `"11"`); Java es fuerte y estático, con
sutilezas por autoboxing y casts.

## 6. Paradigmas en el código (evidencias)
- *Python*: clases `Producto`/`Movimiento`; comprensión de lista y `filter(lambda ...)` en
  `buscar()`; `map(lambda p: p.valor_total, ...)` en `valor_total()`.
- *Java*: encapsulamiento y `final`; Streams (`filter`, `sorted`, `Collectors`), lambdas y
  `Comparator.comparingDouble` en `buscar()`, `alertasBajoInventario()`, `ordenar()`.
- *JavaScript*: clases ES6 + `Map`; estilo funcional con `filter`, `reduce`, `sort` por
  comparador y getters (`get valorTotal()`).

## 7. Arquitectura
Capas equivalentes: **Modelos → Negocio (Inventario) → Persistencia → Presentación**
(CLI + GUI en cada versión). Diferencia estructural: Python usa `dict`, Java usa
`LinkedHashMap`, JS usa `Map`; las tres preservan orden de inserción, por lo que los
reportes son deterministas e idénticos.

## 8. Comparación de estructuras de datos
Python: `dict` + listas, sintaxis mínima. Java: `Map<String,Producto>`,
`List<Movimiento>`, verificación en compilación, más ceremonia. JavaScript: `Map`, arrays y
objetos literales; máxima flexibilidad, menor seguridad.

## 9. Manejo de errores
Python: excepciones (`ValueError`, `KeyError`, `IOError`) con `try/except`. Java:
excepciones de tiempo de ejecución (`IllegalArgumentException`, `NoSuchElementException`);
persistencia con `throws IOException` (chequeadas). JavaScript: `throw new Error(...)` con
`try/catch`; su modelo asíncrono exigiría `Promise`/`async-await` (aquí todo es síncrono).

## 10. Ejecución y compilación
Java paga `javac` a cambio de chequeo temprano y buen rendimiento en caliente. Python y JS
arrancan directo; ambos sacrifican detección de errores de tipos a ejecución salvo
herramientas externas (`mypy`, TypeScript).

## 11. Concurrencia/asincronía
Java: hilos y `java.util.concurrent`. Python: GIL (multiproceso para CPU). JavaScript: un
hilo + bucle de eventos y promesas, ideal para I/O concurrente. En este proyecto (lógica
en memoria + archivos síncronos) las tres se comportan igual.

## 12. Resultados de las pruebas
- Python: **18/18** (unittest). Java: **22/22** (runner propio, sin dependencias). JavaScript: **12/12** (node:test).
- Casos comunes cubiertos en las tres: CRUD, búsqueda, salida excesiva rechazada, alertas,
  valor total, ordenamiento, persistencia redonda JSON↔CSV, lote con rollback.
- Resultados esperados idénticos: valor total inicial **$4521.75**; agotados: P004;
  bajo inventario: P002, P006; mayor valor: P003.

## 13. Ventajas y limitaciones
- Python: rápido de escribir/leer; el rendimiento depende de la carga y del runtime.
- Java: explícito y con verificación estática; requiere JDK.
- JavaScript: expresivo y sin dependencias en Node; la coerción dinámica exige disciplina.

## 14. Mantenibilidad y escalabilidad
La lógica centralizada en `Inventario` permite sustituir CLI/GUI por una API (FastAPI,
Spring, Express) sin tocar reglas. Java escala mejor en equipos grandes por el tipado
estático; Python favorece prototipado; JS unifica lenguaje extremo a extremo.

## 15. Conclusiones
No hay un lenguaje «mejor»: hay uno más adecuado al contexto. La equivalencia funcional
demuestra que las reglas de negocio son independientes de la tecnología, y el ejercicio
evidencia cómo cada lenguaje equilibra seguridad, velocidad de desarrollo y expresividad.

## 16. Referencias
- Python: https://docs.python.org
- Java / Oracle: https://docs.oracle.com/en/java/
- MDN Web Docs: https://developer.mozilla.org
- Node.js: https://nodejs.org/docs
- Todos los códigos del proyecto son originales de este equipo.


## Estado de entrega y límites verificados
La revisión corrigió el arranque de Python y las rutas temporales de las pruebas. Las suites ejecutadas pasaron: Python 18/18, Java 22/22 y JavaScript 12/12. El ZIP no incluía historial Git ni capturas o videos reales; la carpeta de evidencias queda pendiente para que el equipo documente la ejecución de las GUIs. Completa los nombres de integrantes, institución, grupo y docente en la portada antes de entregar. Las reglas de prioridad y tiempos de respuesta descritas en la consigna eran un ejemplo de otro dominio y no forman parte del sistema de inventario recibido.

El repositorio final sigue la estructura académica indicada con carpetas por lenguaje, datos y documentos. Los fixtures productos_pequenos.json, productos_medianos.json y productos_grandes.json contienen subconjuntos de 2, 4 y 6 artículos; todos se conservan dentro del dominio de inventario recibido. Los diagramas de arquitectura y rollback están en formato Mermaid y SVG. El manual técnico complementa este análisis con pasos de instalación, ejecución, pruebas, esquema de datos y límites conocidos.
