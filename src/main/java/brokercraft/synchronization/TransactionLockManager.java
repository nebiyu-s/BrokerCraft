package brokercraft.synchronization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionLockManager {
    private final ConcurrentHashMap<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock getLock(int clientId) {
        return locks.computeIfAbsent(clientId, id -> new ReentrantLock(true));
    }
}
