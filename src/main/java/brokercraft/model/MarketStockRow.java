package brokercraft.model;

import javafx.beans.property.*;

public class MarketStockRow {
    private final StringProperty symbol = new SimpleStringProperty();
    private final StringProperty companyName = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();
    private final DoubleProperty changePercent = new SimpleDoubleProperty();
    private final StringProperty trend = new SimpleStringProperty();

    public MarketStockRow(Stock stock, double changePercent) {
        this.symbol.set(stock.getSymbol());
        this.companyName.set(stock.getCompanyName());
        this.price.set(stock.getPrice());
        this.changePercent.set(changePercent);
        if (changePercent > 0.01) {
            this.trend.set("UP");
        } else if (changePercent < -0.01) {
            this.trend.set("DOWN");
        } else {
            this.trend.set("FLAT");
        }
    }

    public void update(Stock stock, double changePercent) {
        this.symbol.set(stock.getSymbol());
        this.companyName.set(stock.getCompanyName());
        this.price.set(stock.getPrice());
        this.changePercent.set(changePercent);
        if (changePercent > 0.01) {
            this.trend.set("UP");
        } else if (changePercent < -0.01) {
            this.trend.set("DOWN");
        } else {
            this.trend.set("FLAT");
        }
    }

    public String getSymbol() { return symbol.get(); }
    public StringProperty symbolProperty() { return symbol; }

    public String getCompanyName() { return companyName.get(); }
    public StringProperty companyNameProperty() { return companyName; }

    public double getPrice() { return price.get(); }
    public DoubleProperty priceProperty() { return price; }

    public double getChangePercent() { return changePercent.get(); }
    public DoubleProperty changePercentProperty() { return changePercent; }

    public String getTrend() { return trend.get(); }
}
