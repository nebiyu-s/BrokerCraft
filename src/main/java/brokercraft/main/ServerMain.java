package brokercraft.main;

import brokercraft.rmi.BrokerCraftServiceImpl;
import brokercraft.rmi.RMIServer;
import brokercraft.web.AdminWebServer;

/**
 * ServerMain — entry point for the BrokerCraft server process.
 *
 * Starts two services in the same process:
 *
 *   1. RMI Server  (port 1099) — used by JavaFX Broker/Client apps
 *   2. HTTP Server (port 7000) — used by Admin browser dashboard
 *
 * Both share the same PriceSimulator instance so starting/stopping
 * the market from the web dashboard affects all connected JavaFX clients.
 */
public class ServerMain {
    public static void main(String[] args) {
        try {
            // Step 1: Start RMI — connects to DB, binds service
            BrokerCraftServiceImpl rmiService = RMIServer.start();

            // Step 2: Start web server — shares the same PriceSimulator
            AdminWebServer webServer = new AdminWebServer(rmiService.getPriceSimulator());
            webServer.start();

            System.out.println("─────────────────────────────────────────");
            System.out.println("BrokerCraft server is running.");
            System.out.println("  RMI  → localhost:1099  (Broker/Client apps)");
            System.out.println("  HTTP → http://localhost:7000/admin  (Admin browser)");
            System.out.println("Press Ctrl+C to stop.");
            System.out.println("─────────────────────────────────────────");

            // Keep the main thread alive
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Server failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
