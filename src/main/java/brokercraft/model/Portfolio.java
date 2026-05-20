package brokercraft.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Portfolio implements Serializable {
    private static final long serialVersionUID = 1L;

    private int clientId;
    private final List<PortfolioItem> holdings = new ArrayList<>();

    public Portfolio() {}

    public Portfolio(int clientId) {
        this.clientId = clientId;
    }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public List<PortfolioItem> getHoldings() { return holdings; }

    public Optional<PortfolioItem> find(String symbol) {
        return holdings.stream().filter(h -> h.getSymbol().equalsIgnoreCase(symbol)).findFirst();
    }

    public void addShares(String symbol, int qty, double price) {
        Optional<PortfolioItem> existing = find(symbol);
        if (existing.isPresent()) {
            PortfolioItem item = existing.get();
            double totalCost = item.getQuantity() * item.getAveragePrice() + qty * price;
            int newQty = item.getQuantity() + qty;
            item.setQuantity(newQty);
            item.setAveragePrice(newQty == 0 ? 0 : totalCost / newQty);
        } else {
            holdings.add(new PortfolioItem(symbol, qty, price));
        }
    }

    public boolean removeShares(String symbol, int qty) {
        Optional<PortfolioItem> existing = find(symbol);
        if (existing.isEmpty() || existing.get().getQuantity() < qty) {
            return false;
        }
        PortfolioItem item = existing.get();
        item.setQuantity(item.getQuantity() - qty);
        if (item.getQuantity() == 0) {
            holdings.remove(item);
        }
        return true;
    }
}
