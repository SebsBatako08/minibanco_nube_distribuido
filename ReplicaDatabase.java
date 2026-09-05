import java.util.concurrent.ConcurrentHashMap;

public class ReplicaDatabase {

    private final ConcurrentHashMap<Long, Account> accounts =
            new ConcurrentHashMap<>();

    private volatile long lastSequence = 0;

    public ConcurrentHashMap<Long, Account> getAccounts() {
        return accounts;
    }

    public long getLastSequence() {
        return lastSequence;
    }

    public void setLastSequence(long lastSequence) {
        this.lastSequence = lastSequence;
    }
}