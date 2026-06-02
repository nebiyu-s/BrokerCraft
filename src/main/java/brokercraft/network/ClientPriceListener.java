package brokercraft.network;

import brokercraft.model.Stock;
import brokercraft.rmi.PriceUpdateListener;
import javafx.application.Platform;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

/**
 * RMI callback adapter sent to the server so it can push price updates.
 *
 * The listener forwards remote `Stock` updates to a local
 * `Consumer<Stock>` and ensures the consumer runs on the JavaFX
 * Application Thread via `Platform.runLater`.
 */
public class ClientPriceListener extends UnicastRemoteObject implements PriceUpdateListener {
    private static final long serialVersionUID = 1L;
    private final Consumer<Stock> onUpdate;

    public ClientPriceListener(Consumer<Stock> onUpdate) throws RemoteException {
        super();
        this.onUpdate = onUpdate;
    }

    @Override
    public void onPriceUpdate(Stock stock) throws RemoteException {
        Platform.runLater(() -> onUpdate.accept(stock));
    }
}
