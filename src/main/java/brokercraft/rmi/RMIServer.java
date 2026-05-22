package brokercraft.rmi;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public final class RMIServer {
    public static final String SERVICE_NAME = "BrokerCraftService";
    public static final int REGISTRY_PORT = 1099;

    private RMIServer() {}

    /**
     * Start the RMI registry and bind the service.
     * Returns the impl so callers can access shared resources (e.g. PriceSimulator).
     */
    public static BrokerCraftServiceImpl start() throws Exception {
        LocateRegistry.createRegistry(REGISTRY_PORT);
        BrokerCraftServiceImpl service = new BrokerCraftServiceImpl();
        Naming.rebind("//localhost:" + REGISTRY_PORT + "/" + SERVICE_NAME, service);
        System.out.println("RMI registry started on port " + REGISTRY_PORT);
        System.out.println("Service bound: " + SERVICE_NAME);
        return service;
    }
}
