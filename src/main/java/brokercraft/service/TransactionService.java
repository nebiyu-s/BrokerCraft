package brokercraft.service;

import brokercraft.database.DatabaseManager;
import brokercraft.database.Db;
import brokercraft.model.ClientProfile;
import brokercraft.model.Portfolio;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.TransactionType;
import brokercraft.model.User;
import brokercraft.synchronization.TransactionLockManager;
import brokercraft.utils.TransactionLogger;

import java.util.concurrent.locks.ReentrantLock;

public class TransactionService {
    private final DatabaseManager db = DatabaseManager.getInstance();
    private final TransactionLockManager lockManager = new TransactionLockManager();

    public String executeTrade(int clientId, int actingUserId, String symbol, int quantity, boolean isBuy) {
        if (quantity <= 0) {
            return "Quantity must be positive.";
        }

        ReentrantLock lock = lockManager.getLock(clientId);
        lock.lock();
        try {
            return Db.query(() -> {
                var clientUser = db.findUserById(clientId);
                var profileOpt = db.findClientProfile(clientId);
                var stockOpt = db.findStock(symbol);

                if (clientUser.isEmpty() || profileOpt.isEmpty()) {
                    return "Client not found.";
                }
                if (stockOpt.isEmpty()) {
                    return "Stock not found.";
                }

                ClientProfile profile = profileOpt.get();
                Stock stock = stockOpt.get();
                User client = clientUser.get();
                double total = stock.getPrice() * quantity;

                if (isBuy) {
                    if (profile.getBalance() < total) {
                        return "Insufficient balance.";
                    }
                    profile.setBalance(profile.getBalance() - total);
                    Portfolio portfolio = db.getPortfolio(clientId);
                    portfolio.addShares(symbol, quantity, stock.getPrice());
                    db.savePortfolio(portfolio);
                } else {
                    Portfolio portfolio = db.getPortfolio(clientId);
                    if (!portfolio.removeShares(symbol, quantity)) {
                        return "Insufficient shares to sell.";
                    }
                    profile.setBalance(profile.getBalance() + total);
                    db.savePortfolio(portfolio);
                }

                db.saveClientProfile(profile);

                Transaction tx = new Transaction(
                        0,
                        clientId,
                        client.getFullName(),
                        profile.getAssignedBrokerId(),
                        symbol.toUpperCase(),
                        quantity,
                        stock.getPrice(),
                        isBuy ? TransactionType.BUY : TransactionType.SELL
                );
                db.saveTransaction(tx);
                TransactionLogger.log(tx);
                return "SUCCESS";
            });
        } finally {
            lock.unlock();
        }
    }
}
