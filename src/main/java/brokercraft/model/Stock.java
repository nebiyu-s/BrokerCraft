package brokercraft.model;

import java.io.Serializable;

public class Stock implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;
    private String companyName;
    private double price;

    public Stock() {}

    public Stock(String symbol, String companyName, double price) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = price;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
