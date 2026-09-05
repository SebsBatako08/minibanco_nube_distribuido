import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ServidorBanco {

    public static void main(String[] args) {
        try {
            // 1. Inicializamos tu base de datos en memoria (carga los 840,000 registros)
            MiniBanco banco = new MiniBanco();

            // 2. Levantamos el servidor (usamos 8080 para pruebas locales en tu PC)
            int puerto = 8080;
            HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);

            // --- SEGURIDAD Y ACCESO ---
            server.createContext("/api/register", new RegisterHandler(banco));
            server.createContext("/api/login", new LoginHandler(banco));

            // --- OPERACIONES CORE (EL CORAZÓN DEL BANCO LÍDER) ---
            server.createContext("/api/accounts/", new AccountHandler(banco));
            server.createContext("/api/transactions/transfer", new TransferHandler(banco));

            // --- SINCRONIZACIÓN (MÓDULO 2: PARA LAS RÉPLICAS) ---
            server.createContext("/api/sync", new SyncHandler(banco));

            // --- MONITOREO (MÓDULO 3: PARA EL DASHBOARD WEB) ---
            server.createContext("/api/node/status", new StatusHandler(banco));
            server.createContext("/api/storage/count", new StorageCountHandler(banco));

            // 4. Asignamos un Thread Pool para manejar concurrencia masiva
            server.setExecutor(Executors.newFixedThreadPool(20));
            server.start();

            System.out.println("Servidor del Mini Banco iniciado en http://localhost:" + puerto);

        } catch (Exception e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
