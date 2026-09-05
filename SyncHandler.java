import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.SortedMap;

public class SyncHandler implements HttpHandler {
    private MiniBanco banco;

    public SyncHandler(MiniBanco banco) {
        this.banco = banco;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // 1. Validar seguridad (¡No queremos que cualquiera lea el historial!)
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            enviarRespuesta(exchange, 401, "{\"error\": \"No autorizado\"}");
            return;
        }
        
        String token = authHeader.substring(7);
        if (!JwtUtil.validarToken(token)) {
            enviarRespuesta(exchange, 401, "{\"error\": \"Token invalido\"}");
            return;
        }

        // 2. Extraer el parámetro ?from=4500
        String query = exchange.getRequestURI().getQuery();
        int from = 0;
        if (query != null && query.startsWith("from=")) {
            try {
                from = Integer.parseInt(query.split("=")[1]);
            } catch (NumberFormatException e) {
                from = 0;
            }
        }

        // 3. Obtener el historial desde esa secuencia en adelante
        SortedMap<Integer, String> transaccionesNuevas = banco.obtenerTransaccionesDesde(from);

        // 4. Construir el arreglo JSON a mano para máxima velocidad
        StringBuilder jsonArray = new StringBuilder("[");
        int count = 0;
        for (String tx : transaccionesNuevas.values()) {
            jsonArray.append(tx);
            if (++count < transaccionesNuevas.size()) {
                jsonArray.append(",");
            }
        }
        jsonArray.append("]");

        enviarRespuesta(exchange, 200, jsonArray.toString());
    }

    private void enviarRespuesta(HttpExchange exchange, int codigoHTTP, String respuestaJSON) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = respuestaJSON.getBytes();
        exchange.sendResponseHeaders(codigoHTTP, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}