package brokercraft.network;

import brokercraft.model.Stock;
import brokercraft.rmi.PriceUpdateListener;
import javafx.application.Platform;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

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
