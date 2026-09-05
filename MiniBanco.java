import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MiniBanco {
    private ConcurrentHashMap<String, Cuenta> baseDeDatos = new ConcurrentHashMap<>();
    private AtomicInteger contadorSecuencia = new AtomicInteger(0);
    private ConcurrentSkipListMap<Integer, String> historialTransacciones = new ConcurrentSkipListMap<>();
    
    // Variable estática para el Dashboard
    private double saldoTotalHistorico = 0.0;

    // Variables de Cloud Storage
    private Storage storage;
    private final String BUCKET_NAME = "mi-banco-logs-proyecto-distribuido-bancos"; 

    public MiniBanco() {
        System.out.println("Iniciando carga de la base de datos en memoria...");
        cargarRegistrosIniciales();

        try {
            this.storage = StorageOptions.getDefaultInstance().getService();
            System.out.println("Conectado a Google Cloud Storage exitosamente.");
            recuperarEstadoDesdeNube();
        } catch (Exception e) {
            System.out.println("Advertencia: No se pudo conectar a GCS. Iniciando sin respaldo.");
        }
    }

    private void cargarRegistrosIniciales() {
        ArrayList<String> nombres = leerArchivo("nombres.txt");
        ArrayList<String> apellidos = leerArchivo("apellidos.txt");
        Random random = new Random(12345);

        long idCounter = 1;
        double sumaTemporal = 0.0;

        for (String nombre : nombres) {
            for (String ap1 : apellidos) {
                for (String ap2 : apellidos) {
                    double numero = random.nextDouble() * 10000000;
                    numero = Math.round(numero * 10) / 100.0;
                    
                    String propietario = nombre + " " + ap1 + " " + ap2;
                    String id = String.valueOf(idCounter++);

                    Cuenta nuevaCuenta = new Cuenta(id, propietario, numero);
                    baseDeDatos.put(id, nuevaCuenta);
                    sumaTemporal += numero;
                }
            }
        }
        this.saldoTotalHistorico = Math.round(sumaTemporal * 100.0) / 100.0;
        System.out.println("¡Carga exitosa! Total de cuentas: " + baseDeDatos.size());
    }

    private void recuperarEstadoDesdeNube() {
        System.out.println("Buscando bitácora de transacciones en la nube...");
        Iterable<Blob> blobs = storage.list(BUCKET_NAME).iterateAll();
        TreeMap<Integer, String> txOrdenadas = new TreeMap<>();
        
        for (Blob blob : blobs) {
            String nombreFile = blob.getName();
            if (nombreFile.startsWith("tx_") && nombreFile.endsWith(".json")) {
                try {
                    int seq = Integer.parseInt(nombreFile.replace("tx_", "").replace(".json", ""));
                    String json = new String(blob.getContent(), StandardCharsets.UTF_8);
                    txOrdenadas.put(seq, json);
                } catch (Exception e) { }
            }
        }

        for (Map.Entry<Integer, String> entry : txOrdenadas.entrySet()) {
            String json = entry.getValue();
            int secuenciaBackup = entry.getKey();
            
            try {
                String origen = json.split("\"origen\": \"")[1].split("\"")[0];
                String destino = json.split("\"destino\": \"")[1].split("\"")[0];
                String montoStr = json.split("\"monto\": ")[1].split(",")[0].trim();
                double monto = Double.parseDouble(montoStr);
                
                Cuenta cOrigen = baseDeDatos.get(origen);
                Cuenta cDestino = baseDeDatos.get(destino);
                
                if (cOrigen != null && cDestino != null) {
                    cOrigen.retirar(monto);
                    cDestino.depositar(monto);
                    historialTransacciones.put(secuenciaBackup, json);
                    contadorSecuencia.set(Math.max(contadorSecuencia.get(), secuenciaBackup));
                }
            } catch (Exception e) { }
        }
        System.out.println("Estado reconstruido. Última secuencia recuperada: " + contadorSecuencia.get());
    }

    private ArrayList<String> leerArchivo(String nombreArchivo) {
        ArrayList<String> lista = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (!linea.isEmpty()) lista.add(linea);
            }
        } catch (IOException e) {
            System.out.println("Error leyendo archivo: " + nombreArchivo);
        }
        return lista;
    }

    public Cuenta obtenerCuenta(String id) {
        return baseDeDatos.get(id);
    }

    public boolean transferirSeguro(String sourceId, String targetId, double amount) {
        // Prevención de cuentas nulas o transferencias a sí mismo
        if (sourceId.equals(targetId) || amount <= 0) return false;

        Cuenta origen = baseDeDatos.get(sourceId);
        Cuenta destino = baseDeDatos.get(targetId);

        if (origen == null || destino == null) return false;

        // Prevención de deadlocks por ordenamiento
        Cuenta primerLock = sourceId.compareTo(targetId) < 0 ? origen : destino;
        Cuenta segundoLock = sourceId.compareTo(targetId) < 0 ? destino : origen;

        primerLock.getLock().lock();
        try {
            segundoLock.getLock().lock();
            try {
                if (origen.getBalance() >= amount) {
                    origen.retirar(amount);
                    destino.depositar(amount);

                    int secuencia = contadorSecuencia.incrementAndGet();
                    
                    String registroJson = String.format(Locale.US,
                        "{\"secuencia\": %d, \"origen\": \"%s\", \"destino\": \"%s\", \"monto\": %.2f, \"timestamp\": %d}",
                        secuencia, sourceId, targetId, amount, System.currentTimeMillis()
                    );
                    historialTransacciones.put(secuencia, registroJson);

                    // Guardado asíncrono en Cloud Storage
                    if (storage != null) {
                        CompletableFuture.runAsync(() -> {
                            try {
                                BlobId blobId = BlobId.of(BUCKET_NAME, "tx_" + secuencia + ".json");
                                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build();
                                storage.create(blobInfo, registroJson.getBytes(StandardCharsets.UTF_8));
                            } catch (Exception e) {
                                System.err.println("Error subiendo la tx " + secuencia + " a la nube.");
                            }
                        });
                    }
                    return true;
                } else {
                    return false;
                }
            } finally {
                segundoLock.getLock().unlock();
            }
        } finally {
            primerLock.getLock().unlock();
        }
    }

    public SortedMap<Integer, String> obtenerTransaccionesDesde(int fromId) {
        return historialTransacciones.tailMap(fromId + 1);
    }

    public int obtenerTotalTransacciones() {
        return historialTransacciones.size();
    }

    public int obtenerTotalCuentas() {
        return baseDeDatos.size();
    }

    public double obtenerSaldoTotal() {
        return this.saldoTotalHistorico;
    }
}