import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

public class StorageCountHandler implements HttpHandler {
    private MiniBanco banco;

    public StorageCountHandler(MiniBanco banco) {
        this.banco = banco;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Permitir peticiones desde cualquier página web (CORS)
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        int total = banco.obtenerTotalTransacciones();
        String jsonResponse = "{\"count\": " + total + "}";
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = jsonResponse.getBytes();
        exchange.sendResponseHeaders(200, responseBytes.length);
        
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}