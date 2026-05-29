package brokercraft.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import brokercraft.main.SceneRouter;
import brokercraft.main.SessionContext;
import brokercraft.model.IpoListing;
import brokercraft.model.MarketStockRow;
import brokercraft.model.PortfolioItem;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.User;
import brokercraft.network.ClientPriceListener;
import brokercraft.rmi.BrokerCraftService;
import brokercraft.utils.ChartHelper;
import brokercraft.utils.StyleManager;
import brokercraft.utils.UiClock;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ClientDashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label clockLabel;
    @FXML private Label connectionLabel;
    @FXML private Label portfolioValueLabel;
    @FXML private Label balanceLabel;
    @FXML private Label pnlLabel;
    @FXML private Label holdingsCountLabel;
    @FXML private Label brokerLabel;
    @FXML private Label tickerLabel;
    @FXML private Label insightLabel;
    @FXML private Label footerStatusLabel;
    @FXML private Label estimateLabel;
    @FXML private Label selectedStockLabel;
    @FXML private Label portfolioSummaryLabel;
    @FXML private Label portfolioSummaryLabel2;
    @FXML private TabPane mainTabs;
    @FXML private ListView<ChartHelper.AllocationSlice> allocationList;
    @FXML private TextField marketSearchField;
    @FXML private ComboBox<String> marketSortCombo;
    @FXML private TextField txSearchField;
    @FXML private ComboBox<String> txFilterCombo;
    @FXML private TableView<MarketStockRow> marketTable;
    @FXML private TableView<MarketStockRow> marketTableMini;
    @FXML private TableView<MarketStockRow> moversTable;
    @FXML private TableColumn<MarketStockRow, String> stockSymbolCol;
    @FXML private TableColumn<MarketStockRow, String> stockNameCol;
    @FXML private TableColumn<MarketStockRow, Number> stockPriceCol;
    @FXML private TableColumn<MarketStockRow, Number> stockChangeCol;
    @FXML private TableColumn<MarketStockRow, String> stockTrendCol;
    @FXML private TableColumn<MarketStockRow, String> miniSymbolCol;
    @FXML private TableColumn<MarketStockRow, Number> miniPriceCol;
    @FXML private TableColumn<MarketStockRow, Number> miniChangeCol;
    @FXML private TableColumn<MarketStockRow, String> moverSymbolCol;
    @FXML private TableColumn<MarketStockRow, String> moverNameCol;
    @FXML private TableColumn<MarketStockRow, Number> moverPriceCol;
    @FXML private TableColumn<MarketStockRow, Number> moverChangeCol;
    @FXML private TableView<PortfolioRow> portfolioTable;
    @FXML private TableColumn<PortfolioRow, String> portSymbolCol;
    @FXML private TableColumn<PortfolioRow, Number> portQtyCol;
    @FXML private TableColumn<PortfolioRow, Number> portAvgCol;
    @FXML private TableColumn<PortfolioRow, Number> portPriceCol;
    @FXML private TableColumn<PortfolioRow, Number> portValueCol;
    @FXML private TableColumn<PortfolioRow, Number> portPnlCol;
    @FXML private TableView<Transaction> historyTable;
    @FXML private TableColumn<Transaction, String> txTimeCol;
    @FXML private TableColumn<Transaction, String> txTypeCol;
    @FXML private TableColumn<Transaction, String> txSymbolCol;
    @FXML private TableColumn<Transaction, Number> txQtyCol;
    @FXML private TableColumn<Transaction, Number> txPriceCol;
    @FXML private TableColumn<Transaction, String> txTotalCol;
    @FXML private ComboBox<String> tradeSymbolCombo;
    @FXML private TextField quantityField;
    @FXML private Label tradeMessageLabel;

    // ── IPO tab ───────────────────────────────────────────────────────────────
    @FXML private TableView<IpoRow>              ipoTable;
    @FXML private TableColumn<IpoRow, String>    ipoCompanyCol;
    @FXML private TableColumn<IpoRow, String>    ipoSymbolCol;
    @FXML private TableColumn<IpoRow, Number>    ipoSharesCol;
    @FXML private TableColumn<IpoRow, Number>    ipoPriceCol;
    @FXML private TableColumn<IpoRow, String>    ipoDeadlineCol;
    @FXML private TableColumn<IpoRow, String>    ipoDescCol;
    @FXML private ComboBox<String>               ipoSymbolCombo;
    @FXML private TextField                      ipoQuantityField;
    @FXML private Label                          ipoEstimateLabel;
    @FXML private Label                          ipoMessageLabel;

    private final ObservableList<IpoRow> ipoRows = FXCollections.observableArrayList();

    private final ObservableList<MarketStockRow> marketRows = FXCollections.observableArrayList();
    private final ObservableList<PortfolioRow> portfolioRows = FXCollections.observableArrayList();
    private final ObservableList<Transaction> allTransactions = FXCollections.observableArrayList();
    private final Map<String, Double> previousPrices = new HashMap<>();
    private FilteredList<MarketStockRow> filteredMarket;
    private FilteredList<Transaction> filteredTx;
    private BrokerCraftService service;
    private User user;
    private Timeline clock;

    @FXML
    private void initialize() {
        user = SessionContext.getCurrentUser();
        service = SessionContext.getService();
        welcomeLabel.setText(user.getFullName());
        clock = UiClock.bind(clockLabel);
        connectionLabel.setText("*");

        setupMarketTables();
        setupPortfolioTable();
        setupHistoryTable();
        setupFilters();
        setupTradePreview();
        setupIpoTab();

        try {
            registerPriceListener();
            refreshAll();
            footerStatusLabel.setText("Live market feed active");
        } catch (Exception e) {
            StyleManager.setError(tradeMessageLabel, e.getMessage());
            footerStatusLabel.setText("Connection issue");
        }
    }

    private void setupMarketTables() {
        configureMarketColumns(stockSymbolCol, stockNameCol, stockPriceCol, stockChangeCol, stockTrendCol);
        configureMarketColumns(miniSymbolCol, null, miniPriceCol, miniChangeCol, null);
        configureMarketColumns(moverSymbolCol, moverNameCol, moverPriceCol, moverChangeCol, null);

        filteredMarket = new FilteredList<>(marketRows, r -> true);
        SortedList<MarketStockRow> sorted = new SortedList<>(filteredMarket);
        sorted.comparatorProperty().bind(marketTable.comparatorProperty());
        marketTable.setItems(sorted);
        marketTableMini.setItems(marketRows);
        moversTable.setItems(marketRows);

        StyleManager.styleTable(marketTable);
        StyleManager.styleTable(marketTableMini);
        StyleManager.styleTable(moversTable);

        marketTable.getSelectionModel().selectedItemProperty().addListener((o, old, row) -> {
            if (row != null) {
                tradeSymbolCombo.setValue(row.getSymbol());
                selectedStockLabel.setText(row.getSymbol() + "  @  " + StyleManager.formatCurrency(row.getPrice()));
                updateTradeEstimate();
            }
        });
    }

    private void configureMarketColumns(TableColumn<MarketStockRow, String> symCol,
                                        TableColumn<MarketStockRow, String> nameCol,
                                        TableColumn<MarketStockRow, Number> priceCol,
                                        TableColumn<MarketStockRow, Number> changeCol,
                                        TableColumn<MarketStockRow, String> trendCol) {
        if (symCol != null) symCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        if (nameCol != null) nameCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        if (priceCol != null) {
            priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
            priceCol.setCellFactory(c -> currencyCell());
        }
        if (changeCol != null) {
            changeCol.setCellValueFactory(new PropertyValueFactory<>("changePercent"));
            changeCol.setCellFactory(c -> percentCell());
        }
        if (trendCol != null) {
            trendCol.setCellValueFactory(new PropertyValueFactory<>("trend"));
            trendCol.setCellFactory(c -> trendCell());
        }
    }

    private TableCell<MarketStockRow, Number> currencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : StyleManager.formatCurrency(item.doubleValue()));
                setStyle("-fx-text-fill: #93c5fd; -fx-font-weight: bold;");
            }
        };
    }

    private TableCell<MarketStockRow, Number> percentCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                double v = item.doubleValue();
                setText(String.format("%+.2f%%", v));
                if (v > 0) setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
                else if (v < 0) setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                else setStyle("-fx-text-fill: #94a3b8;");
            }
        };
    }

    private TableCell<MarketStockRow, String> trendCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(switch (item) {
                    case "UP" -> "UP";
                    case "DOWN" -> "DN";
                    default -> "-";
                });
                setStyle(switch (item) {
                    case "UP" -> "-fx-text-fill: #4ade80; -fx-font-weight: bold;";
                    case "DOWN" -> "-fx-text-fill: #f87171; -fx-font-weight: bold;";
                    default -> "-fx-text-fill: #94a3b8;";
                });
            }
        };
    }

    private void setupPortfolioTable() {
        portSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        portQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        portAvgCol.setCellValueFactory(new PropertyValueFactory<>("averagePrice"));
        portPriceCol.setCellValueFactory(new PropertyValueFactory<>("marketPrice"));
        portValueCol.setCellValueFactory(new PropertyValueFactory<>("marketValue"));
        portPnlCol.setCellValueFactory(new PropertyValueFactory<>("pnl"));
        portPnlCol.setCellFactory(c -> pnlCell());
        StyleManager.styleTable(portfolioTable);
        portfolioTable.setItems(portfolioRows);
    }

    private TableCell<PortfolioRow, Number> pnlCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                double v = item.doubleValue();
                setText(StyleManager.formatCurrency(v));
                if (v >= 0) setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
                else setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
            }
        };
    }

    private void setupHistoryTable() {
        txTimeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedTime()));
        txTypeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().name()));
        StyleManager.styleTradeSideColumn(txTypeCol);
        txSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        txQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        txPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        txTotalCol.setCellValueFactory(c -> new SimpleStringProperty(
                StyleManager.formatCurrency(c.getValue().getTotal())));
        filteredTx = new FilteredList<>(allTransactions, t -> true);
        historyTable.setItems(filteredTx);
        StyleManager.styleTable(historyTable);
    }

    private void setupFilters() {
        marketSearchField.textProperty().addListener((o, a, b) -> applyMarketFilter());
        marketSortCombo.getItems().addAll("Symbol A-Z", "Price High-Low", "Change % High-Low");
        marketSortCombo.setValue("Symbol A-Z");
        marketSortCombo.valueProperty().addListener((o, a, b) -> applyMarketSort());

        txFilterCombo.getItems().addAll("All", "BUY", "SELL");
        txFilterCombo.setValue("All");
        txFilterCombo.valueProperty().addListener((o, a, b) -> applyTxFilter());
        txSearchField.textProperty().addListener((o, a, b) -> applyTxFilter());
    }

    private void applyMarketFilter() {
        String q = marketSearchField.getText() == null ? "" : marketSearchField.getText().trim().toLowerCase();
        filteredMarket.setPredicate(row ->
                q.isEmpty()
                        || row.getSymbol().toLowerCase().contains(q)
                        || row.getCompanyName().toLowerCase().contains(q));
    }

    private void applyMarketSort() {
        String mode = marketSortCombo.getValue();
        if (mode == null) return;
        Comparator<MarketStockRow> cmp = switch (mode) {
            case "Price High-Low" -> Comparator.comparingDouble(MarketStockRow::getPrice).reversed();
            case "Change % High-Low" -> Comparator.comparingDouble(MarketStockRow::getChangePercent).reversed();
            default -> Comparator.comparing(MarketStockRow::getSymbol);
        };
        FXCollections.sort(marketRows, cmp);
    }

    private void applyTxFilter() {
        String side = txFilterCombo.getValue();
        String sym = txSearchField.getText() == null ? "" : txSearchField.getText().trim().toUpperCase();
        filteredTx.setPredicate(tx -> {
            boolean sideOk = side == null || "All".equals(side) || tx.getType().name().equals(side);
            boolean symOk = sym.isEmpty() || tx.getSymbol().toUpperCase().contains(sym);
            return sideOk && symOk;
        });
    }

    private void setupTradePreview() {
        tradeSymbolCombo.valueProperty().addListener((o, a, b) -> updateTradeEstimate());
        quantityField.textProperty().addListener((o, a, b) -> updateTradeEstimate());
    }

    private void updateTradeEstimate() {
        String sym = tradeSymbolCombo.getValue();
        if (sym == null) {
            estimateLabel.setText("Select a stock to preview order cost.");
            return;
        }
        Optional<Stock> stock = ChartHelper.findStock(toStocks(), sym);
        if (stock.isEmpty()) {
            estimateLabel.setText("Stock not found.");
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
        } catch (Exception e) {
            estimateLabel.setText(stock.get().getSymbol() + " @ " + StyleManager.formatCurrency(stock.get().getPrice()) + " per share");
            return;
        }
        double total = stock.get().getPrice() * qty;
        estimateLabel.setText(String.format("%d x %s = %s (estimated)",
                qty, StyleManager.formatCurrency(stock.get().getPrice()), StyleManager.formatCurrency(total)));
    }

    private List<Stock> toStocks() {
        List<Stock> list = new ArrayList<>();
        for (MarketStockRow row : marketRows) {
            list.add(new Stock(row.getSymbol(), row.getCompanyName(), row.getPrice()));
        }
        return list;
    }

    private void registerPriceListener() throws Exception {
        ClientPriceListener listener = new ClientPriceListener(stock -> {
            double prev = previousPrices.getOrDefault(stock.getSymbol(), stock.getPrice());
            double changePct = prev == 0 ? 0 : ((stock.getPrice() - prev) / prev) * 100;
            previousPrices.put(stock.getSymbol(), stock.getPrice());

            boolean found = false;
            for (int i = 0; i < marketRows.size(); i++) {
                if (marketRows.get(i).getSymbol().equals(stock.getSymbol())) {
                    marketRows.get(i).update(stock, changePct);
                    found = true;
                    break;
                }
            }
            if (!found) {
                marketRows.add(new MarketStockRow(stock, changePct));
            }
            updateTicker();
            try {
                refreshPortfolio();
                updateMetrics();
            } catch (Exception ignored) {}
        });
        SessionContext.setPriceListener(listener);
        service.registerPriceListener(listener);
    }

    private void refreshAll() throws Exception {
        List<Stock> stocks = service.getStocks();
        marketRows.clear();
        previousPrices.clear();
        for (Stock s : stocks) {
            previousPrices.put(s.getSymbol(), s.getPrice());
            marketRows.add(new MarketStockRow(s, 0));
        }
        tradeSymbolCombo.setItems(FXCollections.observableArrayList(
                stocks.stream().map(Stock::getSymbol).toList()));
        if (!stocks.isEmpty()) {
            tradeSymbolCombo.getSelectionModel().selectFirst();
        }
        applyMarketSort();
        updateTicker();
        updateBrokerLabel();
        refreshPortfolio();
        refreshHistory();
        refreshIpos();
        updateMetrics();
        footerStatusLabel.setText("Last sync: " + java.time.LocalTime.now().withNano(0));
    }

    private void updateTicker() {
        String text = marketRows.stream()
                .sorted(Comparator.comparingDouble(MarketStockRow::getChangePercent).reversed())
                .limit(5)
                .map(r -> String.format("%s %s (%+.2f%%)",
                        r.getSymbol(), StyleManager.formatCurrency(r.getPrice()), r.getChangePercent()))
                .collect(Collectors.joining("   |   "));
        tickerLabel.setText(text.isEmpty() ? "Waiting for market data..." : text);
    }

    private void updateBrokerLabel() throws Exception {
        var profile = service.getClientProfile(user.getId());
        if (profile != null && profile.getAssignedBrokerId() != null) {
            // Show broker's actual name instead of just the ID
            User broker = service.getUserById(profile.getAssignedBrokerId());
            brokerLabel.setText(broker != null ? broker.getFullName() : "Broker #" + profile.getAssignedBrokerId());
        } else {
            brokerLabel.setText("Pending");
        }
    }

    private void refreshPortfolio() throws Exception {
        List<Stock> stocks = toStocks();
        List<PortfolioItem> holdings = service.getPortfolio(user.getId());
        portfolioRows.clear();
        for (PortfolioItem item : holdings) {
            double mkt = ChartHelper.findStock(stocks, item.getSymbol())
                    .map(Stock::getPrice).orElse(item.getAveragePrice());
            double value = item.getQuantity() * mkt;
            double cost = item.getQuantity() * item.getAveragePrice();
            portfolioRows.add(new PortfolioRow(item.getSymbol(), item.getQuantity(),
                    item.getAveragePrice(), mkt, value, value - cost));
        }
        ChartHelper.bindAllocationList(allocationList, holdings, stocks);
        updateMetrics();
    }

    private void updateMetrics() throws Exception {
        List<Stock> stocks = toStocks();
        List<PortfolioItem> holdings = service.getPortfolio(user.getId());
        double cash = service.getBalance(user.getId());
        double portVal = ChartHelper.portfolioValue(holdings, stocks);
        double cost = ChartHelper.costBasis(holdings);
        double pnl = portVal - cost;

        portfolioValueLabel.setText(StyleManager.formatCurrency(portVal + cash));
        balanceLabel.setText(StyleManager.formatCurrency(cash));
        pnlLabel.setText((pnl >= 0 ? "+" : "") + StyleManager.formatCurrency(pnl));
        pnlLabel.getStyleClass().removeAll("change-up", "change-down");
        pnlLabel.getStyleClass().add(pnl >= 0 ? "change-up" : "change-down");
        holdingsCountLabel.setText(String.valueOf(holdings.size()));
        portfolioSummaryLabel.setText(holdings.size() + " positions  |  Invested " + StyleManager.formatCurrency(cost));

        if (pnl > 0) {
            insightLabel.setText("Your portfolio is up " + StyleManager.formatCurrency(pnl)
                    + " vs cost basis. Consider taking profits on winners.");
        } else if (pnl < 0) {
            insightLabel.setText("Portfolio is down " + StyleManager.formatCurrency(Math.abs(pnl))
                    + ". Review positions in the Market tab.");
        } else {
            insightLabel.setText("Build your first position using the Trade tab. You have "
                    + StyleManager.formatCurrency(cash) + " cash available.");
        }
    }

    private void refreshHistory() throws Exception {
        allTransactions.setAll(service.getTransactions(user.getId()));
        applyTxFilter();
    }

    @FXML private void onBuy() { executeTrade(true); }
    @FXML private void onSell() { executeTrade(false); }

    private void executeTrade(boolean buy) {
        try {
            String symbol = tradeSymbolCombo.getValue();
            int qty = Integer.parseInt(quantityField.getText().trim());
            String result = service.executeTrade(user.getId(), user.getId(), symbol, qty, buy);
            if ("SUCCESS".equals(result)) {
                StyleManager.setSuccess(tradeMessageLabel,
                        buy ? "Buy order filled successfully." : "Sell order filled successfully.");
                refreshPortfolio();
                refreshHistory();
                updateTradeEstimate();
                mainTabs.getSelectionModel().select(2);
            } else {
                StyleManager.setError(tradeMessageLabel, result);
            }
        } catch (NumberFormatException e) {
            StyleManager.setError(tradeMessageLabel, "Enter a valid share quantity.");
        } catch (Exception e) {
            StyleManager.setError(tradeMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        try {
            refreshAll();
            StyleManager.setInfo(tradeMessageLabel, "All data refreshed.");
        } catch (Exception e) {
            StyleManager.setError(tradeMessageLabel, e.getMessage());
        }
    }

    // ── IPO tab ───────────────────────────────────────────────────────────────

    private void setupIpoTab() {
        ipoCompanyCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().companyName));
        ipoSymbolCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().symbol));
        ipoSharesCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().sharesRemaining));
        ipoPriceCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().pricePerShare));
        ipoDeadlineCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().deadline));
        ipoDescCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().description));
        StyleManager.styleTable(ipoTable);
        ipoTable.setItems(ipoRows);

        // Click IPO row → auto-fill symbol combo
        ipoTable.getSelectionModel().selectedItemProperty().addListener((o, old, row) -> {
            if (row != null) {
                ipoSymbolCombo.setValue(row.symbol);
                updateIpoEstimate();
            }
        });

        ipoSymbolCombo.valueProperty().addListener((o, a, b) -> updateIpoEstimate());
        ipoQuantityField.textProperty().addListener((o, a, b) -> updateIpoEstimate());
    }

    private void refreshIpos() throws Exception {
        List<IpoListing> openIpos = service.getOpenIpos();
        ipoRows.clear();
        List<String> symbols = new ArrayList<>();
        for (IpoListing ipo : openIpos) {
            ipoRows.add(new IpoRow(ipo));
            symbols.add(ipo.getSymbol());
        }
        ipoSymbolCombo.setItems(FXCollections.observableArrayList(symbols));
        if (!symbols.isEmpty() && ipoSymbolCombo.getValue() == null) {
            ipoSymbolCombo.getSelectionModel().selectFirst();
        }
    }

    private void updateIpoEstimate() {
        String sym = ipoSymbolCombo.getValue();
        if (sym == null) { ipoEstimateLabel.setText(""); return; }
        ipoRows.stream().filter(r -> r.symbol.equals(sym)).findFirst().ifPresent(row -> {
            try {
                int qty = Integer.parseInt(ipoQuantityField.getText().trim());
                double total = row.pricePerShare * qty;
                ipoEstimateLabel.setText(qty + " × " + StyleManager.formatCurrency(row.pricePerShare)
                        + " = " + StyleManager.formatCurrency(total));
            } catch (Exception e) {
                ipoEstimateLabel.setText("IPO price: " + StyleManager.formatCurrency(row.pricePerShare) + " / share");
            }
        });
    }

    @FXML
    private void onBuyIpo() {
        String sym = ipoSymbolCombo.getValue();
        if (sym == null || sym.isBlank()) {
            StyleManager.setError(ipoMessageLabel, "Select an IPO symbol.");
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(ipoQuantityField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            StyleManager.setError(ipoMessageLabel, "Enter a valid positive quantity.");
            return;
        }
        try {
            String result = service.buyIpoShares(user.getId(), sym, qty);
            if ("SUCCESS".equals(result)) {
                StyleManager.setSuccess(ipoMessageLabel,
                        "Bought " + qty + " IPO shares of " + sym + " successfully.");
                ipoQuantityField.clear();
                refreshIpos();
                refreshPortfolio();
                refreshHistory();
                updateMetrics();
            } else {
                StyleManager.setError(ipoMessageLabel, result);
            }
        } catch (Exception e) {
            StyleManager.setError(ipoMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onLogout() throws Exception {
        if (clock != null) clock.stop();
        if (SessionContext.getPriceListener() != null) {
            service.unregisterPriceListener(SessionContext.getPriceListener());
        }
        SessionContext.clear();
        SceneRouter.goTo("Login.fxml", "BrokerCraft", 1100, 680);
    }

    public static class PortfolioRow {        private final SimpleStringProperty symbol = new SimpleStringProperty();
        private final SimpleIntegerProperty quantity = new SimpleIntegerProperty();
        private final SimpleDoubleProperty averagePrice = new SimpleDoubleProperty();
        private final SimpleDoubleProperty marketPrice = new SimpleDoubleProperty();
        private final SimpleDoubleProperty marketValue = new SimpleDoubleProperty();
        private final SimpleDoubleProperty pnl = new SimpleDoubleProperty();

        public PortfolioRow(String symbol, int qty, double avg, double mkt, double value, double pnlVal) {
            this.symbol.set(symbol);
            this.quantity.set(qty);
            this.averagePrice.set(avg);
            this.marketPrice.set(mkt);
            this.marketValue.set(value);
            this.pnl.set(pnlVal);
        }

        public String getSymbol() { return symbol.get(); }
        public int getQuantity() { return quantity.get(); }
        public double getAveragePrice() { return averagePrice.get(); }
        public double getMarketPrice() { return marketPrice.get(); }
        public double getMarketValue() { return marketValue.get(); }
        public double getPnl() { return pnl.get(); }
    }

    // ── IPO row model ─────────────────────────────────────────────────────────

    public static class IpoRow {
        final String symbol;
        final String companyName;
        final int    sharesRemaining;
        final double pricePerShare;
        final String deadline;
        final String description;

        IpoRow(IpoListing ipo) {
            this.symbol          = ipo.getSymbol();
            this.companyName     = ipo.getCompanyName();
            this.sharesRemaining = ipo.getSharesRemaining();
            this.pricePerShare   = ipo.getPricePerShare();
            this.deadline        = ipo.getDeadline().toString();
            this.description     = ipo.getDescription() != null ? ipo.getDescription() : "";
        }

        public String getSymbol()          { return symbol; }
        public String getCompanyName()     { return companyName; }
        public int    getSharesRemaining() { return sharesRemaining; }
        public double getPricePerShare()   { return pricePerShare; }
        public String getDeadline()        { return deadline; }
        public String getDescription()     { return description; }
    }
}
