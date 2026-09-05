import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 *
 * En terminal:
 *   javac GeneradorCarga.java
 *   java GeneradorCarga <IP_LIDER> <PUERTO> <NUM_CUENTAS> <NUM_HILOS>
 *
 * Ejemplo:
 *   java GeneradorCarga 34.67.174.42 8080 840000 50
 *
 * Proporcion: 80% lecturas (GET /api/accounts/{id})
 *             20% transferencias (POST /api/transactions/transfer)
 *
 * Al finalizar verifica que la suma de saldos sea la misma que al inicio.
 */
public class GeneradorCarga {

    // ── Configuracion ────────────────────────────────────────────────────────
    static String BASE_URL;
    static int    NUM_CUENTAS  = 840_000;   // total de cuentas en la BD
    static int    NUM_HILOS    = 50;        // hilos concurrentes
    static int    DURACION_SEG = 60;        // duración de la prueba

    static final String USER     = "tester@banco.com";
    static final String PASSWORD = "Test1234!";

    // monto mínimo y máximo por transferencia
    static final double MONTO_MIN = 1.0;
    static final double MONTO_MAX = 100.0;

    // ── Contadores thread-safe ────────────────────────────────────────────────
    static final AtomicLong lecturasExitosas      = new AtomicLong();
    static final AtomicLong lecturasFallidas       = new AtomicLong();
    static final AtomicLong transferenciasExitosas = new AtomicLong();
    static final AtomicLong transferenciasFallidas = new AtomicLong();

    // saldo total capturado al inicio (muestreamos N cuentas)
    static final int      MUESTRA_VERIFICACION = 200;
    static final Map<Integer, Double> saldosIniciales = new ConcurrentHashMap<>();

    // ── Cliente HTTP (reutilizable, con keep-alive) ───────────────────────────
    static HttpClient httpClient;
    static String     jwtToken;
    static Random     rng = new Random();

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {

        // args
        String host   = args.length > 0 ? args[0] : "34.67.174.42";
        String puerto = args.length > 1 ? args[1] : "8080";
        if (args.length > 2) NUM_CUENTAS = Integer.parseInt(args[2]);
        if (args.length > 3) NUM_HILOS   = Integer.parseInt(args[3]);

        BASE_URL = "http://" + host + ":" + puerto;

        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Mini Banco — Generador de Carga Masiva     ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println("  Servidor  : " + BASE_URL);
        System.out.println("  Cuentas   : " + NUM_CUENTAS);
        System.out.println("  Hilos     : " + NUM_HILOS);
        System.out.println("  Duración  : " + DURACION_SEG + " segundos");
        System.out.println();

        // 1. Autenticación
        System.out.print("[1/4] Obteniendo token JWT... ");
        jwtToken = login();
        System.out.println("OK ");

        // 2. Muestra de saldos iniciales para verificación
        System.out.print("[2/4] Capturando muestra de saldos iniciales (" + MUESTRA_VERIFICACION + " cuentas)... ");
        capturarSaldosIniciales();
        System.out.println("OK ");

        // 3. Carga masiva durante 60 segundos
        System.out.println("[3/4] Iniciando carga masiva (80% lecturas / 20% transferencias)...");
        System.out.println();

        ExecutorService pool = Executors.newFixedThreadPool(NUM_HILOS);
        Instant inicio = Instant.now();
        Instant fin    = inicio.plus(Duration.ofSeconds(DURACION_SEG));

        // Imprime progreso cada 10 segundos
        ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor();
        ticker.scheduleAtFixedRate(() -> {
            long elapsed = Duration.between(inicio, Instant.now()).toSeconds();
            System.out.printf("  t=%02ds  lecturas=%,d  transferencias=%,d%n",
                elapsed, lecturasExitosas.get(), transferenciasExitosas.get());
        }, 10, 10, TimeUnit.SECONDS);

        for (int i = 0; i < NUM_HILOS; i++) {
            pool.submit(() -> {
                while (Instant.now().isBefore(fin)) {
                    try {
                        if (rng.nextDouble() < 0.80) {
                            realizarLectura();
                        } else {
                            realizarTransferencia();
                        }
                    } catch (Exception e) {
                        // ignoramos errores de red individuales; los contadores los registran
                    }
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(DURACION_SEG + 10, TimeUnit.SECONDS);
        ticker.shutdownNow();

        // 4. Verificación de consistencia
        System.out.println();
        System.out.println("[4/4] Verificando consistencia de saldos...");
        List<String> inconsistencias = verificarConsistencia();

        // ── Resultados ────────────────────────────────────────────────────────
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║                 RESULTADOS                   ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.printf("  Lecturas exitosas      : %,d%n", lecturasExitosas.get());
        System.out.printf("  Lecturas fallidas       : %,d%n", lecturasFallidas.get());
        System.out.printf("  Transferencias exitosas : %,d%n", transferenciasExitosas.get());
        System.out.printf("  Transferencias fallidas : %,d%n", transferenciasFallidas.get());

        // Puntaje del concurso: 4× transferencias + 1× lecturas
        long puntaje = (transferenciasExitosas.get() * 4L) + lecturasExitosas.get();
        System.out.printf("  Puntaje concurso (4×T + L): %,d%n", puntaje);
        System.out.println();

        if (inconsistencias.isEmpty()) {
            System.out.println("   CONSISTENCIA OK — no se detectaron inconsistencias.");
        } else {
            System.out.println("   INCONSISTENCIAS DETECTADAS en " + inconsistencias.size() + " cuenta(s):");
            for (String msg : inconsistencias) {
                System.out.println("    → " + msg);
            }
        }
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN — POST /api/login
    // ─────────────────────────────────────────────────────────────────────────
    static String login() throws Exception {
        // Primero intenta registrar (puede fallar si ya existe, lo ignoramos)
        try {
            String regBody = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}", USER, PASSWORD);
            post("/api/register", regBody, null);
        } catch (Exception ignored) {}

        String loginBody = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\"}", USER, PASSWORD);
        String resp = post("/api/login", loginBody, null);

        // Extrae el token del JSON: busca el campo "token" o "jwt"
        String token = extractJson(resp, "token");
        if (token == null) token = extractJson(resp, "jwt");
        if (token == null) throw new RuntimeException("No se pudo extraer el token de: " + resp);
        return token;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LECTURA — GET /api/accounts/{id}
    // ─────────────────────────────────────────────────────────────────────────
    static void realizarLectura() throws Exception {
        int id = idAleatorio();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/accounts/" + id))
            .header("Authorization", "Bearer " + jwtToken)
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            lecturasExitosas.incrementAndGet();
        } else {
            lecturasFallidas.incrementAndGet();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRANSFERENCIA — POST /api/transactions/transfer
    // ─────────────────────────────────────────────────────────────────────────
    static void realizarTransferencia() throws Exception {
        int origen  = idAleatorio();
        int destino = idAleatorio();
        while (destino == origen) destino = idAleatorio();  // deben ser distintos

        double monto = MONTO_MIN + (MONTO_MAX - MONTO_MIN) * rng.nextDouble();
        monto = Math.round(monto * 100.0) / 100.0;

        String body = String.format(Locale.US,
            "{\"sourceAccountId\":\"%d\",\"targetAccountId\":\"%d\",\"amount\":%.2f}",
            origen, destino, monto);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/transactions/transfer"))
            .header("Authorization", "Bearer " + jwtToken)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200 || res.statusCode() == 201) {
            transferenciasExitosas.incrementAndGet();
        } else {
            transferenciasFallidas.incrementAndGet();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFICACIÓN DE CONSISTENCIA
    // Consulta los mismos IDs que al inicio y compara el saldo
    // La suma total debe ser la misma (las transferencias solo mueven dinero)
    // ─────────────────────────────────────────────────────────────────────────
    static void capturarSaldosIniciales() throws Exception {
        List<Integer> ids = muestraIds(MUESTRA_VERIFICACION);
        for (int id : ids) {
            Double saldo = consultarSaldo(id);
            if (saldo != null) saldosIniciales.put(id, saldo);
        }
    }

    static List<String> verificarConsistencia() throws Exception {
        List<String> problemas = new ArrayList<>();

        // 1. Verifica que el total de la muestra siga siendo el mismo
        double sumaInicial = saldosIniciales.values().stream().mapToDouble(Double::doubleValue).sum();
        double sumaFinal   = 0.0;

        for (int id : saldosIniciales.keySet()) {
            Double saldo = consultarSaldo(id);
            if (saldo == null) {
                problemas.add("Cuenta " + id + ": no responde al verificar");
            } else {
                sumaFinal += saldo;
            }
        }

        double diff = Math.abs(sumaFinal - sumaInicial);
        if (diff > 0.01) {  // tolerancia de 1 centavo por redondeo de doubles
            problemas.add(String.format(
                "Suma de muestra cambió: inicial=%.2f  final=%.2f  diferencia=%.2f",
                sumaInicial, sumaFinal, diff));
        } else {
            System.out.printf("  Suma muestra inicial : $%,.2f%n", sumaInicial);
            System.out.printf("  Suma muestra final   : $%,.2f%n", sumaFinal);
        }

        return problemas;
    }

    static Double consultarSaldo(int id) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/accounts/" + id))
                .header("Authorization", "Bearer " + jwtToken)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return null;
            String val = extractJson(res.body(), "balance");
            if (val == null) val = extractJson(res.body(), "saldo");
            return val != null ? Double.parseDouble(val) : null;
        } catch (Exception e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────────────────────────────────
    static int idAleatorio() {
        return 1 + rng.nextInt(NUM_CUENTAS);
    }

    static List<Integer> muestraIds(int n) {
        Set<Integer> ids = new HashSet<>();
        while (ids.size() < n) ids.add(idAleatorio());
        return new ArrayList<>(ids);
    }

    /** Extrae el valor de un campo JSON simple (no anidado) sin dependencias externas */
    static String extractJson(String json, String field) {
        if (json == null) return null;
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : null;
        } else {
            int end = start;
            while (end < json.length() && ",}]".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(start, end).trim();
        }
    }

    static String post(String path, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        HttpResponse<String> res = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400)
            throw new RuntimeException("HTTP " + res.statusCode() + ": " + res.body());
        return res.body();
    }
}
