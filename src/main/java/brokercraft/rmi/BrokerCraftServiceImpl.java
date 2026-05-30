package brokercraft.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import brokercraft.database.DatabaseManager;
import brokercraft.database.Db;
import brokercraft.model.ClientProfile;
import brokercraft.model.CompanyProfile;
import brokercraft.model.IpoListing;
import brokercraft.model.Portfolio;
import brokercraft.model.PortfolioItem;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.User;
import brokercraft.model.UserRole;
import brokercraft.service.AuthService;
import brokercraft.service.IpoService;
import brokercraft.service.TransactionService;
import brokercraft.simulation.PriceSimulator;

public class BrokerCraftServiceImpl extends UnicastRemoteObject implements BrokerCraftService {
    private static final long serialVersionUID = 1L;

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final AuthService authService = new AuthService();
    private final TransactionService transactionService = new TransactionService();
    private final IpoService ipoService = new IpoService();
    private final PriceSimulator priceSimulator = new PriceSimulator();
    private final Set<String> activeSessions = new HashSet<>();

    public BrokerCraftServiceImpl() throws RemoteException {
        super();
        db.initialize();
        // Auto-start price simulation on server boot
        priceSimulator.start();
        System.out.println("Price simulation started automatically.");
    }

    /** Expose the simulator so AdminWebServer can share the same instance. */
    public PriceSimulator getPriceSimulator() {
        return priceSimulator;
    }

    private RemoteException remote(Exception e) {
        return new RemoteException(e.getMessage(), e);
    }

    @Override
    public User login(String username, String password, UserRole role) throws RemoteException {
        try {
            User user = authService.login(username, password, role);
            if (user != null) {
                activeSessions.add(username);
            }
            return user;
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public void registerClient(String username, String password, String fullName, String email)
            throws RemoteException {
        try {
            authService.registerClient(username, password, fullName, email);
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public User createBroker(String username, String password, String fullName, String department)
            throws RemoteException {
        try {
            return authService.createBroker(username, password, fullName, department);
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public List<ClientProfile> getPendingClients() throws RemoteException {
        try {
            return Db.query(db::findPendingClients);
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public void approveClient(int clientId, int brokerId) throws RemoteException {
        try {
            authService.approveClient(clientId, brokerId);
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public void rejectClient(int clientId) throws RemoteException {
        try {
            authService.rejectClient(clientId);
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public User getUserById(int userId) throws RemoteException {
        try {
            return Db.query(() -> db.findUserById(userId).orElse(null));
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public List<User> getBrokers() throws RemoteException {
        try {
            return Db.query(() -> db.findUsersByRole(UserRole.BROKER));
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public List<ClientProfile> getClientsForBroker(int brokerId) throws RemoteException {
        try {
            return Db.query(() -> db.findClientsByBroker(brokerId));
        } catch (Exception e) { throw remote(e); }
    }

    @Override
    public void deleteBroker(int brokerId) throws RemoteException {
        try { Db.execute(() -> db.deleteBroker(brokerId)); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public void updateBroker(int brokerId, String fullName, String department) throws RemoteException {
        try {
            Db.execute(() -> {
                var userOpt = db.findUserById(brokerId);
                if (userOpt.isEmpty()) throw new IllegalArgumentException("Broker not found.");
                User user = userOpt.get();
                user.setFullName(fullName);
                db.saveUser(user);
                db.saveBrokerProfile(new brokercraft.model.BrokerProfile(brokerId, department));
            });
        } catch (Exception e) { throw remote(e); }
    }

    @Override
    public void reassignClient(int clientId, int newBrokerId) throws RemoteException {
        try { Db.execute(() -> db.reassignClient(clientId, newBrokerId)); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public List<ClientProfile> getAllApprovedClients() throws RemoteException {
        try { return Db.query(db::findAllApprovedClients); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public List<Stock> getStocks() throws RemoteException {
        try {
            return Db.query(db::getAllStocks);
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public List<PortfolioItem> getPortfolio(int clientId) throws RemoteException {
        try {
            return Db.query(() -> {
                Portfolio p = db.getPortfolio(clientId);
                return new ArrayList<>(p.getHoldings());
            });
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public double getBalance(int clientId) throws RemoteException {
        try {
            return Db.query(() -> db.findClientProfile(clientId).map(ClientProfile::getBalance).orElse(0.0));
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public ClientProfile getClientProfile(int clientId) throws RemoteException {
        try {
            return Db.query(() -> db.findClientProfile(clientId).orElse(null));
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public List<Transaction> getTransactions(int clientId) throws RemoteException {
        try {
            return Db.query(() -> db.getTransactionsForClient(clientId));
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public List<Transaction> getAllTransactions() throws RemoteException {
        try {
            return Db.query(db::getAllTransactions);
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public List<Transaction> getBrokerTransactions(int brokerId) throws RemoteException {
        try {
            return Db.query(() -> db.getTransactionsForBrokerClients(brokerId));
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public String executeTrade(int clientId, int actingUserId, String symbol, int quantity, boolean isBuy)
            throws RemoteException {
        try {
            return transactionService.executeTrade(clientId, actingUserId, symbol, quantity, isBuy);
        } catch (Exception e) {
            throw remote(e);
        }
    }

    @Override
    public void registerPriceListener(PriceUpdateListener listener) throws RemoteException {
        priceSimulator.addListener(listener);
    }

    @Override
    public void unregisterPriceListener(PriceUpdateListener listener) throws RemoteException {
        priceSimulator.removeListener(listener);
    }

    @Override
    public void startPriceSimulation() throws RemoteException {
        priceSimulator.start();
    }

    @Override
    public void stopPriceSimulation() throws RemoteException {
        priceSimulator.stop();
    }

    @Override
    public boolean isPriceSimulationRunning() throws RemoteException {
        return priceSimulator.isRunning();
    }

    @Override
    public List<String> getActiveUsernames() throws RemoteException {
        return new ArrayList<>(activeSessions);
    }

    // ── Company ───────────────────────────────────────────────────────────────

    @Override
    public void registerCompany(String username, String password, String fullName,
                                String email, String description, String industry)
            throws RemoteException {
        try {
            authService.registerCompany(username, password, fullName, email, description, industry);
        } catch (Exception e) { throw remote(e); }
    }

    @Override
    public CompanyProfile getCompanyProfile(int companyId) throws RemoteException {
        try {
            return Db.query(() -> db.findCompanyProfile(companyId).orElse(null));
        } catch (Exception e) { throw remote(e); }
    }

    @Override
    public List<CompanyProfile> getPendingCompanies() throws RemoteException {
        try {
            return Db.query(db::findPendingCompanies);
        } catch (Exception e) { throw remote(e); }
    }

    @Override
    public void approveCompany(int companyId) throws RemoteException {
        try { authService.approveCompany(companyId); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public void rejectCompany(int companyId) throws RemoteException {
        try { authService.rejectCompany(companyId); }
        catch (Exception e) { throw remote(e); }
    }

    // ── IPO ───────────────────────────────────────────────────────────────────

    @Override
    public IpoListing submitIpo(int companyId, String symbol, int sharesOffered,
                                double pricePerShare, String description,
                                String deadline) throws RemoteException {
        try {
            // Accept both YYYY-MM-DD and YYYY-M-D formats
            java.time.LocalDate date;
            try {
                date = java.time.LocalDate.parse(deadline);
            } catch (Exception e) {
                // Try parsing with single-digit month/day
                date = java.time.LocalDate.parse(deadline,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-M-d"));
            }
            return ipoService.submitIpo(companyId, symbol, sharesOffered,
                    pricePerShare, description, date);
        } catch (Exception e) { throw remote(e); }
    }

    @Override
    public List<IpoListing> getPendingIpos() throws RemoteException {
        try { return ipoService.getPendingIpos(); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public List<IpoListing> getOpenIpos() throws RemoteException {
        try { return ipoService.getOpenIpos(); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public List<IpoListing> getAllIpos() throws RemoteException {
        try { return ipoService.getAllIpos(); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public List<IpoListing> getIposForCompany(int companyId) throws RemoteException {
        try { return ipoService.getIposForCompany(companyId); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public void approveIpo(int ipoId) throws RemoteException {
        try { ipoService.approveIpo(ipoId); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public void rejectIpo(int ipoId) throws RemoteException {
        try { ipoService.rejectIpo(ipoId); }
        catch (Exception e) { throw remote(e); }
    }

    @Override
    public String buyIpoShares(int clientId, String symbol, int quantity) throws RemoteException {
        try { return ipoService.buyIpoShares(clientId, symbol, quantity); }
        catch (Exception e) { throw remote(e); }
    }
}
