package brokercraft.model;

import java.io.Serializable;

public class PortfolioItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;
    private int quantity;
    private double averagePrice;

    public PortfolioItem() {}

    public PortfolioItem(String symbol, int quantity, double averagePrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }

    public double getMarketValue(double currentPrice) {
        return quantity * currentPrice;
    }
}
