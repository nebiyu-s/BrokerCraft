package brokercraft.web;

import brokercraft.database.DatabaseManager;
import brokercraft.database.Db;
import brokercraft.model.ClientProfile;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.User;
import brokercraft.model.UserRole;
import brokercraft.rmi.BrokerCraftServiceImpl;
import brokercraft.service.AuthService;
import brokercraft.simulation.PriceSimulator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminWebServer — runs an embedded HTTP server on port 7000.
 *
 * The Admin opens http://localhost:7000/admin in their browser.
 * All data is loaded via JSON API calls from the browser using plain JavaScript.
 *
 * Architecture:
 *   Browser (Admin) ──HTTP──► AdminWebServer ──► DatabaseManager / AuthService
 *                                             ──► PriceSimulator
 */
public class AdminWebServer {

    public static final int PORT = 7000;

    // Gson with LocalDateTime support for JSON serialization
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(
                                    src.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
            .create();

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final AuthService authService = new AuthService();
    private final PriceSimulator priceSimulator;
    private Javalin app;

    public AdminWebServer(PriceSimulator priceSimulator) {
        this.priceSimulator = priceSimulator;
    }

    /** Start the web server. Called from ServerMain. */
    public void start() {
        app = Javalin.create(config -> {
            // Allow browser to call our API (CORS)
            config.bundledPlugins.enableCors(cors ->
                    cors.addRule(rule -> rule.anyHost()));
        });

        // ── Serve the Admin HTML dashboard ──────────────────────────────────
        app.get("/admin", ctx -> {
            ctx.contentType("text/html");
            ctx.result(AdminHtmlPage.build());
        });

        // ── API: read data ───────────────────────────────────────────────────
        app.get("/api/brokers",      this::getBrokers);
        app.get("/api/pending",      this::getPendingClients);
        app.get("/api/transactions", this::getTransactions);
        app.get("/api/stocks",       this::getStocks);
        app.get("/api/sessions",     this::getSessions);
        app.get("/api/stats",        this::getStats);

        // ── API: actions ─────────────────────────────────────────────────────
        app.post("/api/brokers/create",    this::createBroker);
        app.post("/api/clients/approve",   this::approveClient);
        app.post("/api/clients/reject",    this::rejectClient);
        app.post("/api/simulation/start",  this::startSimulation);
        app.post("/api/simulation/stop",   this::stopSimulation);

        app.start(PORT);
        System.out.println("Admin web dashboard → http://localhost:" + PORT + "/admin");
    }

    public void stop() {
        if (app != null) app.stop();
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    /** GET /api/brokers — list all broker users */
    private void getBrokers(Context ctx) {
        List<User> brokers = Db.query(() -> db.findUsersByRole(UserRole.BROKER));
        ctx.json(GSON.toJson(brokers));
    }

    /** GET /api/pending — list clients waiting for approval */
    private void getPendingClients(Context ctx) {
        try {
            List<ClientProfile> pending = Db.query(db::findPendingClients);
            // Enrich with full name from users table
            List<Map<String, Object>> result = pending.stream().map(cp -> {
                Map<String, Object> row = new HashMap<>();
                row.put("userId", cp.getUserId());
                row.put("email", cp.getEmail());
                row.put("status", cp.getStatus().name());
                User u = Db.query(() -> db.findUserById(cp.getUserId()).orElse(null));
                row.put("fullName", u != null ? u.getFullName() : "Unknown");
                row.put("username", u != null ? u.getUsername() : "?");
                return row;
            }).toList();
            ctx.json(GSON.toJson(result));
        } catch (Exception e) {
            ctx.status(500).json(error(e.getMessage()));
        }
    }

    /** GET /api/transactions — all platform transactions */
    private void getTransactions(Context ctx) {
        List<Transaction> txs = Db.query(db::getAllTransactions);
        ctx.json(GSON.toJson(txs));
    }

    /** GET /api/stocks — current stock prices */
    private void getStocks(Context ctx) {
        List<Stock> stocks = Db.query(db::getAllStocks);
        ctx.json(GSON.toJson(stocks));
    }

    /** GET /api/sessions — placeholder (active session count from RMI impl) */
    private void getSessions(Context ctx) {
        // Sessions are tracked in BrokerCraftServiceImpl; we return a simple count here
        ctx.json("{\"count\": 0}");
    }

    /** GET /api/stats — summary numbers for the dashboard header cards */
    private void getStats(Context ctx) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount",  Db.query(db::findPendingClients).size());
        stats.put("brokerCount",   Db.query(() -> db.findUsersByRole(UserRole.BROKER)).size());
        stats.put("txCount",       Db.query(db::getAllTransactions).size());
        stats.put("stockCount",    Db.query(db::getAllStocks).size());
        stats.put("simRunning",    priceSimulator.isRunning());
        ctx.json(GSON.toJson(stats));
    }

    /** POST /api/brokers/create — body: {username, password, fullName, department} */
    private void createBroker(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            String username   = body.get("username").getAsString();
            String password   = body.get("password").getAsString();
            String fullName   = body.get("fullName").getAsString();
            String department = body.get("department").getAsString();
            User broker = authService.createBroker(username, password, fullName, department);
            ctx.json(GSON.toJson(broker));
        } catch (Exception e) {
            ctx.status(400).json(error(e.getMessage()));
        }
    }

    /** POST /api/clients/approve — body: {clientId, brokerId} */
    private void approveClient(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            int clientId = body.get("clientId").getAsInt();
            int brokerId = body.get("brokerId").getAsInt();
            authService.approveClient(clientId, brokerId);
            ctx.json(ok("Client approved."));
        } catch (Exception e) {
            ctx.status(400).json(error(e.getMessage()));
        }
    }

    /** POST /api/clients/reject — body: {clientId} */
    private void rejectClient(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            int clientId = body.get("clientId").getAsInt();
            authService.rejectClient(clientId);
            ctx.json(ok("Client rejected."));
        } catch (Exception e) {
            ctx.status(400).json(error(e.getMessage()));
        }
    }

    /** POST /api/simulation/start */
    private void startSimulation(Context ctx) {
        priceSimulator.start();
        ctx.json(ok("Simulation started."));
    }

    /** POST /api/simulation/stop */
    private void stopSimulation(Context ctx) {
        priceSimulator.stop();
        ctx.json(ok("Simulation stopped."));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String ok(String message) {
        return "{\"success\": true, \"message\": \"" + message + "\"}";
    }

    private String error(String message) {
        return "{\"success\": false, \"error\": \"" + message + "\"}";
    }
}
