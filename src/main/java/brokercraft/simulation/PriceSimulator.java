package brokercraft.simulation;

import brokercraft.database.DatabaseManager;
import brokercraft.model.Stock;
import brokercraft.rmi.PriceUpdateListener;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class PriceSimulator implements Runnable {
    private static final int INTERVAL_MS = 3000;
    private static final double MAX_CHANGE_PCT = 0.05;

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final Random random = new Random();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<PriceUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private Thread thread;

    public void addListener(PriceUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PriceUpdateListener listener) {
        listeners.remove(listener);
    }

    public boolean isRunning() {
        return running.get();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            thread = new Thread(this, "PriceSimulator");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                tick();
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void tick() {
        try {
            List<Stock> stocks = db.getAllStocks();
            for (Stock stock : stocks) {
                double change = 1 + (random.nextDouble() * 2 - 1) * MAX_CHANGE_PCT;
                double newPrice = Math.max(1, stock.getPrice() * change);
                stock.setPrice(Math.round(newPrice * 100.0) / 100.0);
                db.saveStock(stock);
                notifyListeners(stock);
            }
        } catch (Exception e) {
            System.err.println("Price simulation tick failed: " + e.getMessage());
        }
    }

    private void notifyListeners(Stock stock) {
        for (PriceUpdateListener listener : listeners) {
            try {
                listener.onPriceUpdate(stock);
            } catch (Exception e) {
                System.err.println("Price listener error: " + e.getMessage());
            }
        }
    }
}
