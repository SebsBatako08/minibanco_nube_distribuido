import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Cuenta {
    private String id;
    private String propietario;
    private double balance;
    
    private final Lock lock = new ReentrantLock();

    public Cuenta(String id, String propietario, double balance) {
        this.id = id;
        this.propietario = propietario;
        this.balance = balance;
    }

    public String getId() { return id; }
    public String getPropietario() { return propietario; }
    
    // Lectura protegida contra lecturas fantasma
    public double getBalance() { 
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }
    
    public Lock getLock() { return lock; }

    // Evitamos la pérdida de decimales de Java
    public void retirar(double monto) {
        this.balance = Math.round((this.balance - monto) * 100.0) / 100.0;
    }

    public void depositar(double monto) {
        this.balance = Math.round((this.balance + monto) * 100.0) / 100.0;
    }
}