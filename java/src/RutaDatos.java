import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Localiza los archivos compartidos sin depender del directorio de arranque. */
public final class RutaDatos {
    private RutaDatos() { }

    public static Path archivo(String nombre) throws IOException {
        Path directorio = Path.of("").toAbsolutePath().normalize();
        while (directorio != null) {
            Path candidato = directorio.resolve("datos").resolve(nombre);
            if (Files.isRegularFile(candidato)) return candidato;
            directorio = directorio.getParent();
        }
        throw new IOException("No se encontró datos/" + nombre + " desde " + Path.of("").toAbsolutePath());
    }
}
