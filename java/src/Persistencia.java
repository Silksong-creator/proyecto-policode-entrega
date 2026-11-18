import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Carga y guardado de datos en JSON y CSV (Java, sin dependencias externas). */
public class Persistencia {

    // ---------- JSON mínimo (escritura) ----------
    public static void guardarProductosJson(Collection<Producto> ps, String ruta) throws IOException {
        StringBuilder sb = new StringBuilder("[\n");
        Iterator<Producto> it = ps.iterator();
        while (it.hasNext()) {
            Producto p = it.next();
            sb.append(String.format(Locale.US,
                "  {\"codigo\": \"%s\", \"nombre\": \"%s\", \"categoria\": \"%s\", \"precio\": %.2f, \"existencia\": %d, \"stock_minimo\": %d}%s\n",
                p.getCodigo(), p.getNombre(), p.getCategoria(),
                p.getPrecio(), p.getExistencia(), p.getStockMinimo(),
                it.hasNext() ? "," : ""));
        }
        sb.append("]");
        escribir(ruta, sb.toString());
    }

    // ---------- CSV ----------
    public static void guardarProductosCsv(Collection<Producto> ps, String ruta) throws IOException {
        StringBuilder sb = new StringBuilder("codigo,nombre,categoria,precio,existencia,stock_minimo\n");
        for (Producto p : ps)
            sb.append(String.format(Locale.US, "%s,%s,%s,%.2f,%d,%d%n",
                p.getCodigo(), p.getNombre(), p.getCategoria(),
                p.getPrecio(), p.getExistencia(), p.getStockMinimo()));
        escribir(ruta, sb.toString());
    }

    public static List<Producto> cargarProductosCsv(String ruta) throws IOException {
        List<Producto> productos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta, StandardCharsets.UTF_8))) {
            br.readLine(); // encabezado
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] c = linea.split(",");
                productos.add(new Producto(c[0], c[1], c[2],
                    Double.parseDouble(c[3]), Integer.parseInt(c[4]), Integer.parseInt(c[5])));
            }
        }
        return productos;
    }

    private static void escribir(String ruta, String contenido) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(ruta), StandardCharsets.UTF_8)) {
            w.write(contenido);
        }
    }
}
