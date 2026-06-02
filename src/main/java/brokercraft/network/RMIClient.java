package brokercraft.network;

import brokercraft.rmi.BrokerCraftService;
import brokercraft.rmi.RMIServer;

import java.rmi.Naming;

/**
 * Small helper that performs a lazy RMI lookup for the remote
 * `BrokerCraftService` and caches the result for subsequent calls.
 *
 * Call `reset()` to clear the cached reference (for example when the
 * connection is lost and a fresh lookup is required).
 */
public final class RMIClient {
    private static BrokerCraftService service;

    private RMIClient() {}

    /**
     * Lookup the remote BrokerCraftService once and cache it.
     */
    public static BrokerCraftService connect() throws Exception {
        if (service == null) {
            String url = "//localhost:" + RMIServer.REGISTRY_PORT + "/" + RMIServer.SERVICE_NAME;
            service = (BrokerCraftService) Naming.lookup(url);
        }
        return service;
    }

    /** Clear the cached service reference so the next `connect()` will re-lookup. */
    public static void reset() {
        service = null;
    }
}
