package brokercraft.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import brokercraft.model.ClientProfile;
import brokercraft.model.CompanyProfile;
import brokercraft.model.IpoListing;
import brokercraft.model.PortfolioItem;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.User;
import brokercraft.model.UserRole;

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

    void deleteBroker(int brokerId) throws RemoteException;

    void updateBroker(int brokerId, String fullName, String department) throws RemoteException;

    void reassignClient(int clientId, int newBrokerId) throws RemoteException;

    List<ClientProfile> getAllApprovedClients() throws RemoteException;

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

    // ── Company registration ──────────────────────────────────────────────────
    void registerCompany(String username, String password, String fullName,
                         String email, String description, String industry) throws RemoteException;

    CompanyProfile getCompanyProfile(int companyId) throws RemoteException;

    // ── Admin: company approval ───────────────────────────────────────────────
    List<CompanyProfile> getPendingCompanies() throws RemoteException;
    void approveCompany(int companyId) throws RemoteException;
    void rejectCompany(int companyId)  throws RemoteException;

    // ── IPO lifecycle ─────────────────────────────────────────────────────────
    IpoListing submitIpo(int companyId, String symbol, int sharesOffered,
                         double pricePerShare, String description,
                         String deadline) throws RemoteException;

    List<IpoListing> getPendingIpos()                  throws RemoteException;
    List<IpoListing> getOpenIpos()                     throws RemoteException;
    List<IpoListing> getAllIpos()                       throws RemoteException;
    List<IpoListing> getIposForCompany(int companyId)  throws RemoteException;

    void approveIpo(int ipoId) throws RemoteException;
    void rejectIpo(int ipoId)  throws RemoteException;

    // ── Client: buy IPO shares ────────────────────────────────────────────────
    String buyIpoShares(int clientId, String symbol, int quantity) throws RemoteException;
}
