package brokercraft.service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import brokercraft.database.DatabaseManager;
import brokercraft.database.Db;
import brokercraft.model.ClientProfile;
import brokercraft.model.IpoListing;
import brokercraft.model.Portfolio;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.TransactionType;
import brokercraft.model.User;
import brokercraft.synchronization.TransactionLockManager;
import brokercraft.utils.TransactionLogger;

/**
 * IpoService — handles all IPO-related business logic.
 *
 * Three main operations:
 *
 * 1. submitIpo()    — company submits an IPO request (status = PENDING)
 * 2. approveIpo()   — admin approves it (status = OPEN, stock row created)
 * 3. buyIpoShares() — client buys shares during the IPO (status stays OPEN
 *                     until all shares sold or deadline passes)
 */
public class IpoService {

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final TransactionLockManager lockManager = new TransactionLockManager();

    // ── Company: submit IPO ───────────────────────────────────────────────────

    /**
     * Called by the company dashboard when they submit an IPO.
     *
     * Validates:
     *  - symbol is not already used in stocks or another open IPO
     *  - sharesOffered > 0
     *  - pricePerShare > 0
     *  - deadline is in the future
     */
    public IpoListing submitIpo(int companyId, String sym, int sharesOffered,
                                double pricePerShare, String description, LocalDate deadline) {
        return Db.query(() -> {
            String symbol = sym.toUpperCase().trim();

            if (sharesOffered <= 0)   throw new IllegalArgumentException("Shares offered must be positive.");
            if (pricePerShare <= 0)   throw new IllegalArgumentException("Price per share must be positive.");
            if (deadline.isBefore(LocalDate.now().plusDays(1)))
                throw new IllegalArgumentException("Deadline must be at least tomorrow.");

            // Symbol must not already exist in stocks table
            if (db.findStock(symbol).isPresent())
                throw new IllegalArgumentException("Symbol '" + symbol + "' is already listed on the market.");

            // Symbol must not already have an open/pending IPO
            var existing = db.findIpoBySymbol(symbol);
            if (existing.isPresent() && existing.get().getStatus() != IpoListing.IpoStatus.REJECTED)
                throw new IllegalArgumentException("An IPO for symbol '" + symbol + "' already exists.");

            User company = db.findUserById(companyId)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found."));

            IpoListing ipo = new IpoListing(
                    0, companyId, company.getFullName(), symbol,
                    sharesOffered, sharesOffered, pricePerShare,
                    description, deadline, IpoListing.IpoStatus.PENDING);

            return db.saveIpoListing(ipo);
        });
    }

    // ── Admin: approve IPO ────────────────────────────────────────────────────

    /**
     * Admin approves an IPO.
     *
     * What happens:
     *  1. IPO status → OPEN
     *  2. A new row is inserted into 'stocks' so the symbol appears in the market
     *     (price = IPO price per share)
     *  3. Price simulator will start fluctuating this stock automatically
     */
    public void approveIpo(int ipoId) {
        Db.execute(() -> {
            IpoListing ipo = findIpoById(ipoId);
            if (ipo.getStatus() != IpoListing.IpoStatus.PENDING)
                throw new IllegalStateException("IPO is not in PENDING state.");

            ipo.setStatus(IpoListing.IpoStatus.OPEN);
            db.updateIpoListing(ipo);

            // Create the stock so it appears in the live market
            Stock stock = new Stock(ipo.getSymbol(), ipo.getCompanyName(), ipo.getPricePerShare());
            db.saveStock(stock);
        });
    }

    /**
     * Admin rejects an IPO.
     */
    public void rejectIpo(int ipoId) {
        Db.execute(() -> {
            IpoListing ipo = findIpoById(ipoId);
            ipo.setStatus(IpoListing.IpoStatus.REJECTED);
            db.updateIpoListing(ipo);
        });
    }

    // ── Client: buy IPO shares ────────────────────────────────────────────────

    /**
     * Client buys shares during an open IPO.
     *
     * This is different from a normal trade:
     *  - Shares come from the IPO pool (sharesRemaining decreases)
     *  - Price is fixed at IPO price (not the live market price)
     *  - If all shares are sold, IPO status → CLOSED
     *
     * Returns "SUCCESS" or an error message string.
     */
    public String buyIpoShares(int clientId, String symbol, int quantity) {
        if (quantity <= 0) return "Quantity must be positive.";

        ReentrantLock lock = lockManager.getLock(clientId);
        lock.lock();
        try {
            return Db.query(() -> {
                // Find the open IPO for this symbol
                var ipoOpt = db.findIpoBySymbol(symbol);
                if (ipoOpt.isEmpty() || ipoOpt.get().getStatus() != IpoListing.IpoStatus.OPEN)
                    return "No open IPO found for symbol: " + symbol;

                IpoListing ipo = ipoOpt.get();

                // Check deadline
                if (LocalDate.now().isAfter(ipo.getDeadline())) {
                    ipo.setStatus(IpoListing.IpoStatus.CLOSED);
                    db.updateIpoListing(ipo);
                    return "IPO deadline has passed.";
                }

                // Check available shares
                if (ipo.getSharesRemaining() < quantity)
                    return "Only " + ipo.getSharesRemaining() + " shares remaining in IPO.";

                // Check client balance
                var profileOpt = db.findClientProfile(clientId);
                var userOpt    = db.findUserById(clientId);
                if (profileOpt.isEmpty() || userOpt.isEmpty()) return "Client not found.";

                ClientProfile profile = profileOpt.get();
                User client = userOpt.get();
                double total = ipo.getPricePerShare() * quantity;

                if (profile.getBalance() < total)
                    return "Insufficient balance. Need " + total + " ETB.";

                // Deduct balance
                profile.setBalance(profile.getBalance() - total);
                db.saveClientProfile(profile);

                // Add shares to client portfolio
                Portfolio portfolio = db.getPortfolio(clientId);
                portfolio.addShares(symbol, quantity, ipo.getPricePerShare());
                db.savePortfolio(portfolio);

                // Reduce IPO shares remaining
                ipo.setSharesRemaining(ipo.getSharesRemaining() - quantity);
                if (ipo.getSharesRemaining() == 0) {
                    ipo.setStatus(IpoListing.IpoStatus.CLOSED);
                }
                db.updateIpoListing(ipo);

                // Record transaction
                Transaction tx = new Transaction(
                        0, clientId, client.getFullName(),
                        profile.getAssignedBrokerId(),
                        symbol.toUpperCase(), quantity,
                        ipo.getPricePerShare(), TransactionType.BUY);
                db.saveTransaction(tx);
                TransactionLogger.log(tx);

                return "SUCCESS";
            });
        } finally {
            lock.unlock();
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<IpoListing> getPendingIpos()          { return Db.query(db::findPendingIpos); }
    public List<IpoListing> getOpenIpos()             { return Db.query(db::findOpenIpos); }
    public List<IpoListing> getAllIpos()               { return Db.query(db::getAllIpos); }
    public List<IpoListing> getIposForCompany(int id) { return Db.query(() -> db.findIposByCompany(id)); }

    // ── Helper ────────────────────────────────────────────────────────────────

    private IpoListing findIpoById(int ipoId) throws java.sql.SQLException {
        return db.getAllIpos().stream()
                .filter(i -> i.getId() == ipoId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("IPO not found: " + ipoId));
    }
}
