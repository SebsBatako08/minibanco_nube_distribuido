import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class AccountHandler implements HttpHandler {
    private MiniBanco banco;

    public AccountHandler(MiniBanco banco) {
        this.banco = banco;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Solo permitimos peticiones GET
        if ("GET".equals(exchange.getRequestMethod())) {
            
            // 1. EL CADENERO: Extraer y validar el JWT del Header
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                enviarRespuesta(exchange, 401, "{\"error\": \"No autorizado. Falta token.\"}");
                return;
            }
            
            // Quitamos la palabra "Bearer " para dejar solo el token puro
            String token = authHeader.substring(7);
            if (!JwtUtil.validarToken(token)) {
                enviarRespuesta(exchange, 401, "{\"error\": \"Token invalido o expirado.\"}");
                return;
            }

            // 2. Extraer el {id} de la URL (ej. /api/accounts/125)
            String path = exchange.getRequestURI().getPath();
            String[] partes = path.split("/");
            
            // Si la URL no tiene la estructura correcta
            if (partes.length < 4) {
                enviarRespuesta(exchange, 400, "{\"error\": \"Falta el ID de la cuenta.\"}");
                return;
            }
            
            String idCuenta = partes[3];

            // 3. Buscar en nuestra base de datos en memoria RAM
            Cuenta cuenta = banco.obtenerCuenta(idCuenta);
            if (cuenta == null) {
                enviarRespuesta(exchange, 404, "{\"error\": \"Cuenta no encontrada.\"}");
                return;
            }

            // 4. Retornar el JSON exacto que pide el PDF
            String jsonResponse = String.format(Locale.US, 
                "{\n  \"id\": %s,\n  \"propietario\": \"%s\",\n  \"balance\": %.2f\n}",
                cuenta.getId(), cuenta.getPropietario(), cuenta.getBalance()
            );
            
            enviarRespuesta(exchange, 200, jsonResponse);
        } else {
            // Si mandan un POST u otra cosa a esta ruta, lo bloqueamos
            exchange.sendResponseHeaders(405, -1);
        }
    }

    // Método auxiliar para no repetir código al enviar respuestas HTTP
    private void enviarRespuesta(HttpExchange exchange, int codigoHTTP, String respuestaJSON) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(codigoHTTP, respuestaJSON.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(respuestaJSON.getBytes());
        os.close();
    }
}