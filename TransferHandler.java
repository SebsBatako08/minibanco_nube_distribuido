import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.TopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class TransferHandler implements HttpHandler {
    private MiniBanco banco;
    private Gson gson = new Gson();
    private Publisher publisher;

    // CONFIGURACIÓN DE GOOGLE CLOUD
    // Reemplaza esto con el ID de tu proyecto y el nombre de tu tópico en Google Cloud Console
    private static final String PROJECT_ID = "proyecto-distribuido-bancos"; 
    private static final String TOPIC_ID = "transferencias-topic";

    public TransferHandler(MiniBanco banco) {
        this.banco = banco;
        try {
            // Inicializamos el publicador una sola vez para mantener alto rendimiento
            TopicName topicName = TopicName.of(PROJECT_ID, TOPIC_ID);
            this.publisher = Publisher.newBuilder(topicName).build();
            System.out.println("-> Conexión con Google Pub/Sub inicializada correctamente.");
        } catch (Exception e) {
            System.err.println("-> Alerta: No se pudo conectar a Pub/Sub (revisa tus credenciales locales): " + e.getMessage());
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // 1. Validar el Token JWT
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

        try {
            // 2. Leer el JSON del cuerpo de la petición
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            JsonObject body = gson.fromJson(isr, JsonObject.class);

            String sourceId = body.get("sourceAccountId").getAsString();
            String targetId = body.get("targetAccountId").getAsString();
            double amount = body.get("amount").getAsDouble();

            // 3. Ejecutar la transferencia segura en memoria RAM (Locks)
            boolean exito = banco.transferirSeguro(sourceId, targetId, amount);

            if (exito) {
                // 4. PUBLICAR EN GOOGLE CLOUD PUB/SUB
                // Creamos el mensaje en formato JSON para el sistema de auditoría
                String mensajeAuditoria = String.format(
                    "{\"evento\": \"TRANSFERENCIA\", \"origen\": \"%s\", \"destino\": \"%s\", \"monto\": %.2f, \"timestamp\": %d}",
                    sourceId, targetId, amount, System.currentTimeMillis()
                );

                if (publisher != null) {
                    ByteString data = ByteString.copyFromUtf8(mensajeAuditoria);
                    PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
                    
                    // La publicación es asíncrona, no detiene el hilo principal del banco
                    publisher.publish(pubsubMessage); 
                }

                enviarRespuesta(exchange, 200, "{\"status\": \"Transferencia exitosa\"}");
            } else {
                enviarRespuesta(exchange, 400, "{\"error\": \"Fondos insuficientes o cuenta invalida\"}");
            }
        } catch (Exception e) {
            enviarRespuesta(exchange, 400, "{\"error\": \"Formato JSON incorrecto\"}");
        }
    }

    private void enviarRespuesta(HttpExchange exchange, int codigoHTTP, String respuestaJSON) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(codigoHTTP, respuestaJSON.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(respuestaJSON.getBytes());
        os.close();
    }
}