package brokercraft.rmi;

import brokercraft.model.Stock;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PriceUpdateListener extends Remote {
    void onPriceUpdate(Stock stock) throws RemoteException;
}
