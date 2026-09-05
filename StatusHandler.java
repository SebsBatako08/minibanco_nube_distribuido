import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class StatusHandler implements HttpHandler {

    private final MiniBanco banco;

    public StatusHandler(MiniBanco banco) {
        this.banco = banco;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // Métricas reales del sistema operativo de la VM
        OperatingSystemMXBean os =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double cpu = os.getCpuLoad() * 100.0;
        if (cpu < 0) cpu = 0;

        long ramTotal = os.getTotalMemorySize();
        long ramLibre = os.getFreeMemorySize();
        double ram = ramTotal > 0 ? ((double)(ramTotal - ramLibre) / ramTotal) * 100.0 : 0;

        java.io.File root = new java.io.File("/");
        double disco = root.getTotalSpace() > 0
            ? ((double)(root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace()) * 100.0
            : 0;

        String jsonResponse = String.format(
            "{\"status\":\"ONLINE\",\"role\":\"LEADER\",\"version\":\"1.0\"," +
            "\"totalCuentas\":%d,\"saldoTotal\":%.2f," +
            "\"transferencias\":%d,\"ultimaTransaccion\":%d," +
            "\"cpu\":%.1f,\"ram\":%.1f,\"disco\":%.1f}",
            banco.obtenerTotalCuentas(),
            banco.obtenerSaldoTotal(),
            banco.obtenerTotalTransacciones(),
            banco.obtenerTotalTransacciones(),
            cpu, ram, disco
        );

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = jsonResponse.getBytes();
        exchange.sendResponseHeaders(200, responseBytes.length);

        OutputStream os2 = exchange.getResponseBody();
        os2.write(responseBytes);
        os2.close();
    }
}
