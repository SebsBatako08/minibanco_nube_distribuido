import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

public class LoginHandler implements HttpHandler {
    private MiniBanco banco;

    public LoginHandler(MiniBanco banco) {
        this.banco = banco;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Solo permitimos peticiones POST
        if ("POST".equals(exchange.getRequestMethod())) {
            
            // Aquí en un sistema real leeríamos el usuario/contraseña del body,
            // pero para esta prueba de rendimiento generaremos el token directamente.
            String token = JwtUtil.generarToken("usuario_concurso");
            
            String jsonResponse = "{\"token\": \"" + token + "\"}";
            
            // Enviamos el código HTTP 200 OK
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
            
            OutputStream os = exchange.getResponseBody();
            os.write(jsonResponse.getBytes());
            os.close();
        } else {
            // Si mandan un GET u otra cosa, bloqueamos con 405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }
}