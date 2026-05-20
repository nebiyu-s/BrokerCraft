package brokercraft.network;

import brokercraft.rmi.BrokerCraftService;
import brokercraft.rmi.RMIServer;

import java.rmi.Naming;

public final class RMIClient {
    private static BrokerCraftService service;

    private RMIClient() {}

    public static BrokerCraftService connect() throws Exception {
        if (service == null) {
            String url = "//localhost:" + RMIServer.REGISTRY_PORT + "/" + RMIServer.SERVICE_NAME;
            service = (BrokerCraftService) Naming.lookup(url);
        }
        return service;
    }

    public static void reset() {
        service = null;
    }
}
