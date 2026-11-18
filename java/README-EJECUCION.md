# Ejecutar la versión Java

Requisito: JDK 17 o superior. Ejecuta desde la carpeta `java/src` para que las rutas relativas al catálogo compartido resuelvan correctamente.

```powershell
cd java/src
javac -encoding UTF-8 *.java
java Main

cd ../tests
javac -encoding UTF-8 -cp ../src -d . InventarioTest.java
java -cp "../src;." InventarioTest
```

Para iniciar la GUI Swing, compila con `javac -encoding UTF-8 *.java` y ejecuta `java InventarioGUI` desde `java/src`.
