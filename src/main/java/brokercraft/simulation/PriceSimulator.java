package brokercraft.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import brokercraft.database.DatabaseManager;
import brokercraft.model.Stock;
import brokercraft.rmi.PriceUpdateListener;

/**
 * PriceSimulator — realistic stock price simulation using a random walk model.
 *
 * Model: Geometric Brownian Motion (simplified)
 *
 *   new_price = current_price × exp(drift + volatility × Z)
 *
 * Where:
 *   drift      = tiny directional bias per tick (slightly positive = market tends up)
 *   volatility = how much the price can move per tick
 *   Z          = gaussian random number (mean=0, std=1)
 *
 * Parameters chosen to simulate realistic intraday movement:
 *   - Tick interval: 3 seconds
 *   - Volatility: 0.003 (0.3% per tick max typical move)
 *   - Drift: +0.00005 (very slight upward bias, like a growing market)
 *   - Price floor: 20% of starting price (stock can't collapse to zero)
 *   - Price ceiling: 400% of starting price (stock can't go to infinity)
 *
 * Each stock also has its own "momentum" — a small carry-over from the
 * previous tick direction, making price movements look more natural
 * (trending) rather than pure noise.
 *
 * Comparison with old model:
 *   Old: random ±5% every 3 seconds → unrealistic, prices swing wildly
 *   New: random ±0.3% with momentum → realistic intraday movement
 */
public class PriceSimulator implements Runnable {

    // ── Simulation parameters ─────────────────────────────────────────────────

    /** How often prices update (milliseconds) */
    private static final int INTERVAL_MS = 3_000;

    /**
     * Volatility per tick.
     * 0.003 = 0.3% typical move per 3-second tick.
     * Over a full trading day (8 hours = 9,600 ticks) this gives
     * roughly 0.3% × sqrt(9600) ≈ 29% annual volatility — realistic
     * for an emerging market like Ethiopia.
     */
    private static final double VOLATILITY = 0.003;

    /**
     * Drift per tick — very slight upward bias.
     * 0.00005 per tick × 9600 ticks/day ≈ +0.48% daily drift.
     * This simulates a growing economy.
     */
    private static final double DRIFT = 0.00005;

    /**
     * Momentum factor — how much the previous tick's direction carries over.
     * 0.15 = 15% of last move direction is carried into next tick.
     * Makes prices trend briefly rather than pure random noise.
     */
    private static final double MOMENTUM = 0.15;

    // ── State ─────────────────────────────────────────────────────────────────

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final Random random = new Random();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<PriceUpdateListener> listeners = new CopyOnWriteArrayList<>();

    /** Starting prices — used to enforce floor/ceiling bounds */
    private final Map<String, Double> startingPrices = new HashMap<>();

    /** Last tick's return for each stock — used for momentum */
    private final Map<String, Double> lastReturn = new HashMap<>();

    private Thread thread;

    // ── Public API ────────────────────────────────────────────────────────────

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

    // ── Simulation loop ───────────────────────────────────────────────────────

    @Override
    public void run() {
        // Snapshot starting prices on first run
        try {
            List<Stock> stocks = db.getAllStocks();
            for (Stock s : stocks) {
                startingPrices.put(s.getSymbol(), s.getPrice());
                lastReturn.put(s.getSymbol(), 0.0);
            }
        } catch (Exception e) {
            System.err.println("PriceSimulator init failed: " + e.getMessage());
        }

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
                double newPrice = calculateNewPrice(stock);
                stock.setPrice(newPrice);
                db.saveStock(stock);
                notifyListeners(stock);
            }
        } catch (Exception e) {
            System.err.println("Price simulation tick failed: " + e.getMessage());
        }
    }

    /**
     * Calculate the next price using Geometric Brownian Motion with momentum.
     *
     * Formula:
     *   return = drift + volatility × (gaussian_noise + momentum × last_return)
     *   new_price = current_price × exp(return)
     *
     * Then clamp to [20% of start, 400% of start].
     */
    private double calculateNewPrice(Stock stock) {
        String symbol = stock.getSymbol();
        double currentPrice = stock.getPrice();

        // Get starting price (use current if not recorded yet)
        double startPrice = startingPrices.getOrDefault(symbol, currentPrice);
        if (!startingPrices.containsKey(symbol)) {
            startingPrices.put(symbol, currentPrice);
        }

        // Gaussian random number (Box-Muller approximation via nextGaussian)
        double gaussianNoise = random.nextGaussian();

        // Momentum: blend current noise with last tick's direction
        double prevReturn = lastReturn.getOrDefault(symbol, 0.0);
        double blendedNoise = gaussianNoise + MOMENTUM * prevReturn;

        // Geometric Brownian Motion return
        double tickReturn = DRIFT + VOLATILITY * blendedNoise;

        // Store this return for next tick's momentum
        lastReturn.put(symbol, tickReturn);

        // Apply return
        double newPrice = currentPrice * Math.exp(tickReturn);

        // Enforce price bounds: floor = 20% of start, ceiling = 400% of start
        double floor   = startPrice * 0.20;
        double ceiling = startPrice * 4.00;
        newPrice = Math.max(floor, Math.min(ceiling, newPrice));

        // Round to 2 decimal places
        return Math.round(newPrice * 100.0) / 100.0;
    }

    private void notifyListeners(Stock stock) {
        for (PriceUpdateListener listener : listeners) {
            try {
                listener.onPriceUpdate(stock);
            } catch (Exception e) {
                System.err.println("Price listener error (client disconnected): " + e.getMessage());
                listeners.remove(listener);
            }
        }
    }
}
