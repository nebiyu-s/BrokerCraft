package brokercraft.rmi;

import brokercraft.model.ClientProfile;
import brokercraft.model.PortfolioItem;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.User;
import brokercraft.model.UserRole;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BrokerCraftService extends Remote {
    User login(String username, String password, UserRole role) throws RemoteException;

    void registerClient(String username, String password, String fullName, String email) throws RemoteException;

    User createBroker(String username, String password, String fullName, String department) throws RemoteException;

    List<ClientProfile> getPendingClients() throws RemoteException;

    void approveClient(int clientId, int brokerId) throws RemoteException;

    void rejectClient(int clientId) throws RemoteException;

    User getUserById(int userId) throws RemoteException;

    List<User> getBrokers() throws RemoteException;

    List<ClientProfile> getClientsForBroker(int brokerId) throws RemoteException;

    List<Stock> getStocks() throws RemoteException;

    List<PortfolioItem> getPortfolio(int clientId) throws RemoteException;

    double getBalance(int clientId) throws RemoteException;

    ClientProfile getClientProfile(int clientId) throws RemoteException;

    List<Transaction> getTransactions(int clientId) throws RemoteException;

    List<Transaction> getAllTransactions() throws RemoteException;

    List<Transaction> getBrokerTransactions(int brokerId) throws RemoteException;

    String executeTrade(int clientId, int actingUserId, String symbol, int quantity,
                        boolean isBuy) throws RemoteException;

    void registerPriceListener(PriceUpdateListener listener) throws RemoteException;

    void unregisterPriceListener(PriceUpdateListener listener) throws RemoteException;

    void startPriceSimulation() throws RemoteException;

    void stopPriceSimulation() throws RemoteException;

    boolean isPriceSimulationRunning() throws RemoteException;

    List<String> getActiveUsernames() throws RemoteException;
}
