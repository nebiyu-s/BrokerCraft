package brokercraft.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import brokercraft.database.DatabaseManager;
import brokercraft.database.Db;
import brokercraft.model.ClientProfile;
import brokercraft.model.CompanyProfile;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.User;
import brokercraft.model.UserRole;
import brokercraft.service.AuthService;
import brokercraft.service.IpoService;
import brokercraft.simulation.PriceSimulator;
import io.javalin.Javalin;
import io.javalin.http.Context;

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
    private final IpoService  ipoService  = new IpoService();
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
        app.get("/api/brokers",           this::getBrokers);
        app.get("/api/pending",           this::getPendingClients);
        app.get("/api/transactions",      this::getTransactions);
        app.get("/api/stocks",            this::getStocks);
        app.get("/api/sessions",          this::getSessions);
        app.get("/api/stats",             this::getStats);
        app.get("/api/companies/pending", this::getPendingCompanies);
        app.get("/api/ipos/pending",      this::getPendingIpos);
        app.get("/api/ipos/all",          this::getAllIpos);
        app.get("/api/clients/approved",  this::getApprovedClients);

        // ── API: actions ─────────────────────────────────────────────────────
        app.post("/api/admin/login",        this::adminLogin);
        app.post("/api/brokers/create",     this::createBroker);
        app.post("/api/brokers/update",     this::updateBroker);
        app.post("/api/brokers/delete",     this::deleteBroker);
        app.post("/api/clients/approve",    this::approveClient);
        app.post("/api/clients/reject",     this::rejectClient);
        app.post("/api/clients/reassign",   this::reassignClient);
        app.post("/api/companies/approve",  this::approveCompany);
        app.post("/api/companies/reject",   this::rejectCompany);
        app.post("/api/ipos/approve",       this::approveIpo);
        app.post("/api/ipos/reject",        this::rejectIpo);
        app.post("/api/simulation/start",   this::startSimulation);
        app.post("/api/simulation/stop",    this::stopSimulation);
        app.start(PORT);
        System.out.println("Admin web dashboard → http://localhost:" + PORT + "/admin");
    }

    public void stop() {
        if (app != null) app.stop();
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    /**
     * POST /api/admin/login — body: {username, password}
     * Validates that the user exists, password matches, and role is ADMIN.
     */
    private void adminLogin(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            String username = body.get("username").getAsString();
            String password = body.get("password").getAsString();

            var userOpt = Db.query(() -> db.findUserByUsername(username));
            if (userOpt.isEmpty()
                    || !userOpt.get().getPassword().equals(password)
                    || userOpt.get().getRole() != UserRole.ADMIN) {
                ctx.status(401).json("{\"success\":false,\"error\":\"Invalid admin credentials.\"}");
                return;
            }
            User admin = userOpt.get();
            ctx.json("{\"success\":true,\"fullName\":\"" + admin.getFullName() + "\"}");
        } catch (Exception e) {
            ctx.status(500).json(error(e.getMessage()));
        }
    }

    /** GET /api/brokers — list all broker users with client count */
    private void getBrokers(Context ctx) {
        try {
            List<User> brokers = Db.query(() -> db.findUsersByRole(UserRole.BROKER));
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (User b : brokers) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",         b.getId());
                row.put("fullName",   b.getFullName());
                row.put("username",   b.getUsername());
                row.put("role",       b.getRole().name());
                row.put("active",     b.isActive());
                // Get department
                var profile = Db.query(() -> db.findBrokerProfile(b.getId()).orElse(null));
                row.put("department", profile != null ? profile.getDepartment() : "General");
                // Get client count
                int clientCount = Db.query(() -> db.findClientsByBroker(b.getId())).size();
                row.put("clientCount", clientCount);
                result.add(row);
            }
            ctx.json(GSON.toJson(result));
        } catch (Exception e) {
            ctx.status(500).json(error(e.getMessage()));
        }
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

    /** GET /api/clients/approved — all approved clients for reassignment */
    private void getApprovedClients(Context ctx) {
        try {
            List<ClientProfile> clients = Db.query(db::findAllApprovedClients);
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (ClientProfile cp : clients) {
                Map<String, Object> row = new HashMap<>();
                row.put("userId",    cp.getUserId());
                row.put("brokerId",  cp.getAssignedBrokerId());
                User u = Db.query(() -> db.findUserById(cp.getUserId()).orElse(null));
                row.put("fullName",  u != null ? u.getFullName() : "?");
                row.put("username",  u != null ? u.getUsername() : "?");
                result.add(row);
            }
            ctx.json(GSON.toJson(result));
        } catch (Exception e) {
            ctx.status(500).json(error(e.getMessage()));
        }
    }

    /** POST /api/brokers/update — body: {brokerId, fullName, department} */
    private void updateBroker(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            int    brokerId   = body.get("brokerId").getAsInt();
            String fullName   = body.get("fullName").getAsString();
            String department = body.get("department").getAsString();
            var userOpt = Db.query(() -> db.findUserById(brokerId));
            if (userOpt.isEmpty()) { ctx.status(404).json(error("Broker not found.")); return; }
            User user = userOpt.get();
            user.setFullName(fullName);
            Db.execute(() -> db.saveUser(user));
            Db.execute(() -> db.saveBrokerProfile(
                    new brokercraft.model.BrokerProfile(brokerId, department)));
            ctx.json(ok("Broker updated."));
        } catch (Exception e) {
            ctx.status(400).json(error(e.getMessage()));
        }
    }

    /** POST /api/brokers/delete — body: {brokerId} */
    private void deleteBroker(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            int brokerId = body.get("brokerId").getAsInt();
            Db.execute(() -> db.deleteBroker(brokerId));
            ctx.json(ok("Broker deleted. Their clients have been unassigned."));
        } catch (Exception e) {
            ctx.status(400).json(error(e.getMessage()));
        }
    }

    /** POST /api/clients/reassign — body: {clientId, brokerId} */
    private void reassignClient(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            int clientId = body.get("clientId").getAsInt();
            int brokerId = body.get("brokerId").getAsInt();
            Db.execute(() -> db.reassignClient(clientId, brokerId));
            ctx.json(ok("Client reassigned successfully."));
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

    /** GET /api/companies/pending */
    private void getPendingCompanies(Context ctx) {
        try {
            List<CompanyProfile> list = Db.query(db::findPendingCompanies);
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (CompanyProfile cp : list) {
                Map<String, Object> row = new HashMap<>();
                row.put("userId",      cp.getUserId());
                row.put("email",       cp.getEmail());
                row.put("industry",    cp.getIndustry());
                row.put("description", cp.getDescription());
                row.put("status",      cp.getStatus().name());
                User u = Db.query(() -> db.findUserById(cp.getUserId()).orElse(null));
                row.put("fullName", u != null ? u.getFullName() : "Unknown");
                row.put("username", u != null ? u.getUsername() : "?");
                result.add(row);
            }
            ctx.json(GSON.toJson(result));
        } catch (Exception e) {
            ctx.status(500).json(error(e.getMessage()));
        }
    }

    /** POST /api/companies/approve — body: {companyId} */
    private void approveCompany(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            authService.approveCompany(body.get("companyId").getAsInt());
            ctx.json(ok("Company approved."));
        } catch (Exception e) {
            ctx.status(400).json(error(e.getMessage()));
        }
    }

    /** POST /api/companies/reject — body: {companyId} */
    private void rejectCompany(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            authService.rejectCompany(body.get("companyId").getAsInt());
            ctx.json(ok("Company rejected."));
        } catch (Exception e) {
            ctx.status(400).json(error(e.getMessage()));
        }
    }

    /** GET /api/ipos/pending */
    private void getPendingIpos(Context ctx) {
        ctx.json(GSON.toJson(ipoService.getPendingIpos()));
    }

    /** GET /api/ipos/all */
    private void getAllIpos(Context ctx) {
        ctx.json(GSON.toJson(ipoService.getAllIpos()));
    }

    /** POST /api/ipos/approve — body: {ipoId} */
    private void approveIpo(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            ipoService.approveIpo(body.get("ipoId").getAsInt());
            ctx.json(ok("IPO approved. Stock is now live on the market."));
        } catch (Exception e) {
            ctx.status(400).json(error(e.getMessage()));
        }
    }

    /** POST /api/ipos/reject — body: {ipoId} */
    private void rejectIpo(Context ctx) {
        try {
            JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
            ipoService.rejectIpo(body.get("ipoId").getAsInt());
            ctx.json(ok("IPO rejected."));
        } catch (Exception e) {
            ctx.status(400).json(error(e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String ok(String message) {
        return "{\"success\": true, \"message\": \"" + message + "\"}";
    }

    private String error(String message) {
        return "{\"success\": false, \"error\": \"" + message + "\"}";
    }
}
