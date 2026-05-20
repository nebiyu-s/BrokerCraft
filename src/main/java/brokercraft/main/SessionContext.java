package brokercraft.main;

import brokercraft.model.User;
import brokercraft.rmi.BrokerCraftService;
import brokercraft.rmi.PriceUpdateListener;

public final class SessionContext {
    private static User currentUser;
    private static BrokerCraftService service;
    private static PriceUpdateListener priceListener;

    private SessionContext() {}

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    public static BrokerCraftService getService() { return service; }
    public static void setService(BrokerCraftService svc) { service = svc; }

    public static PriceUpdateListener getPriceListener() { return priceListener; }
    public static void setPriceListener(PriceUpdateListener listener) { priceListener = listener; }

    public static void clear() {
        currentUser = null;
        service = null;
        priceListener = null;
    }
}
