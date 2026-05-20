package brokercraft.utils;

import brokercraft.model.PortfolioItem;
import brokercraft.model.Stock;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ChartHelper {
    private ChartHelper() {}

    public record AllocationSlice(String symbol, double value, double percent) {}

    public static List<AllocationSlice> computeAllocation(List<PortfolioItem> holdings, List<Stock> market) {
        Map<String, Double> prices = new HashMap<>();
        for (Stock s : market) {
            prices.put(s.getSymbol().toUpperCase(), s.getPrice());
        }
        List<AllocationSlice> slices = new ArrayList<>();
        double total = 0;
        for (PortfolioItem item : holdings) {
            double price = prices.getOrDefault(item.getSymbol().toUpperCase(), item.getAveragePrice());
            double value = item.getQuantity() * price;
            if (value > 0) {
                total += value;
                slices.add(new AllocationSlice(item.getSymbol(), value, 0));
            }
        }
        if (total <= 0) {
            slices.add(new AllocationSlice("Cash / Empty", 1, 100));
            return slices;
        }
        double finalTotal = total;
        return slices.stream()
                .map(s -> new AllocationSlice(s.symbol(), s.value(), (s.value() / finalTotal) * 100))
                .sorted(Comparator.comparingDouble(AllocationSlice::value).reversed())
                .toList();
    }

    public static void bindAllocationList(ListView<AllocationSlice> list, List<PortfolioItem> holdings, List<Stock> market) {
        ObservableList<AllocationSlice> items = FXCollections.observableArrayList(computeAllocation(holdings, market));
        list.setItems(items);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AllocationSlice slice, boolean empty) {
                super.updateItem(slice, empty);
                if (empty || slice == null) {
                    setGraphic(null);
                    return;
                }
                Label sym = new Label(slice.symbol());
                sym.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-min-width: 80;");
                Label pct = new Label(String.format("%.1f%%", slice.percent()));
                pct.setStyle("-fx-text-fill: #94a3b8; -fx-min-width: 52;");
                Label val = new Label(StyleManager.formatCurrency(slice.value()));
                val.setStyle("-fx-text-fill: #93c5fd; -fx-font-weight: bold;");
                ProgressBar bar = new ProgressBar(slice.percent() / 100.0);
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.getStyleClass().add("allocation-bar");
                HBox.setHgrow(bar, Priority.ALWAYS);
                HBox top = new HBox(12, sym, pct, val);
                top.setAlignment(Pos.CENTER_LEFT);
                VBox box = new VBox(6, top, bar);
                box.setPadding(new Insets(4, 0, 8, 0));
                setGraphic(box);
            }
        });
    }

    public static double portfolioValue(List<PortfolioItem> holdings, List<Stock> market) {
        Map<String, Double> prices = new HashMap<>();
        for (Stock s : market) {
            prices.put(s.getSymbol().toUpperCase(), s.getPrice());
        }
        double total = 0;
        for (PortfolioItem item : holdings) {
            double price = prices.getOrDefault(item.getSymbol().toUpperCase(), item.getAveragePrice());
            total += item.getQuantity() * price;
        }
        return total;
    }

    public static double costBasis(List<PortfolioItem> holdings) {
        return holdings.stream()
                .mapToDouble(h -> h.getQuantity() * h.getAveragePrice())
                .sum();
    }

    public static Optional<Stock> findStock(List<Stock> stocks, String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }
        return stocks.stream()
                .filter(s -> s.getSymbol().equalsIgnoreCase(symbol))
                .findFirst();
    }
}
