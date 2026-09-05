import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

public class RegisterHandler implements HttpHandler {
    private MiniBanco banco;

    public RegisterHandler(MiniBanco banco) {
        this.banco = banco;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Solo permitimos peticiones POST para el registro
        if ("POST".equals(exchange.getRequestMethod())) {
            
            // En un sistema real, aquí leeríamos el JSON con los datos del nuevo usuario
            // y lo guardaríamos en una base de datos de credenciales.
            
            String jsonResponse = "{\"status\": \"Usuario registrado exitosamente\"}";
            
            // Enviamos el código HTTP 201 (Created)
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, jsonResponse.getBytes().length);
            
            OutputStream os = exchange.getResponseBody();
            os.write(jsonResponse.getBytes());
            os.close();
        } else {
            // Si mandan un GET u otra cosa, bloqueamos con 405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }
}