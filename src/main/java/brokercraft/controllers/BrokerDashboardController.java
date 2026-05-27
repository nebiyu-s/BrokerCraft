package brokercraft.controllers;

import java.time.LocalDate;
import java.util.List;

import brokercraft.main.SceneRouter;
import brokercraft.main.SessionContext;
import brokercraft.model.ClientProfile;
import brokercraft.model.PortfolioItem;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.User;
import brokercraft.network.ClientPriceListener;
import brokercraft.rmi.BrokerCraftService;
import brokercraft.utils.ChartHelper;
import brokercraft.utils.StyleManager;
import brokercraft.utils.UiClock;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class BrokerDashboardController {

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label clockLabel;
    @FXML private Label clientCountLabel;
    @FXML private Label clientAumLabel;
    @FXML private Label tradesTodayLabel;
    @FXML private Label footerLabel;

    // ── Sidebar ───────────────────────────────────────────────────────────────
    @FXML private TextField      clientSearchField;
    @FXML private ListView<String> clientsList;
    @FXML private Label          selectedClientLabel;
    @FXML private Label          clientEmailLabel;
    @FXML private Label          clientBalanceLabel;

    // ── Market table ──────────────────────────────────────────────────────────
    @FXML private TableView<Stock>              marketTable;
    @FXML private TableColumn<Stock, String>    stockSymbolCol;
    @FXML private TableColumn<Stock, String>    stockNameCol;
    @FXML private TableColumn<Stock, Number>    stockPriceCol;

    // ── Portfolio table (with P&L) ────────────────────────────────────────────
    @FXML private TableView<PortfolioRow>           portfolioTable;
    @FXML private TableColumn<PortfolioRow, String> portSymbolCol;
    @FXML private TableColumn<PortfolioRow, Number> portQtyCol;
    @FXML private TableColumn<PortfolioRow, Number> portAvgCol;
    @FXML private TableColumn<PortfolioRow, Number> portPriceCol;
    @FXML private TableColumn<PortfolioRow, Number> portValueCol;
    @FXML private TableColumn<PortfolioRow, Number> portPnlCol;

    // ── Trade panel ───────────────────────────────────────────────────────────
    @FXML private ComboBox<String> tradeSymbolCombo;
    @FXML private TextField        quantityField;
    @FXML private Label            brokerEstimateLabel;
    @FXML private CheckBox         confirmCheck;
    @FXML private Label            tradeMessageLabel;

    // ── Client history tab ────────────────────────────────────────────────────
    @FXML private Label                         historyClientLabel;
    @FXML private ComboBox<String>              historyFilterCombo;
    @FXML private TableView<Transaction>        historyTable;
    @FXML private TableColumn<Transaction, String> htTimeCol;
    @FXML private TableColumn<Transaction, String> htTypeCol;
    @FXML private TableColumn<Transaction, String> htSymbolCol;
    @FXML private TableColumn<Transaction, Number> htQtyCol;
    @FXML private TableColumn<Transaction, Number> htPriceCol;
    @FXML private TableColumn<Transaction, String> htTotalCol;

    // ── All activity tab ──────────────────────────────────────────────────────
    @FXML private ComboBox<String>              activityFilterCombo;
    @FXML private TableView<Transaction>        txTable;
    @FXML private TableColumn<Transaction, String> txTimeCol;
    @FXML private TableColumn<Transaction, String> txClientCol;
    @FXML private TableColumn<Transaction, String> txTypeCol;
    @FXML private TableColumn<Transaction, String> txSymbolCol;
    @FXML private TableColumn<Transaction, Number> txQtyCol;
    @FXML private TableColumn<Transaction, Number> txPriceCol;
    @FXML private TableColumn<Transaction, String> txTotalCol;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ObservableList<Stock>        stocks       = FXCollections.observableArrayList();
    private final ObservableList<String>       allClients   = FXCollections.observableArrayList();
    private final ObservableList<PortfolioRow> portfolioRows = FXCollections.observableArrayList();
    private final ObservableList<Transaction>  allTx        = FXCollections.observableArrayList();
    private final ObservableList<Transaction>  clientTx     = FXCollections.observableArrayList();
    private FilteredList<String>      filteredClients;
    private FilteredList<Transaction> filteredTx;
    private FilteredList<Transaction> filteredClientTx;
    private BrokerCraftService service;
    private User   broker;
    private int    selectedClientId   = -1;
    private String selectedClientName = "";

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        broker  = SessionContext.getCurrentUser();
        service = SessionContext.getService();
        welcomeLabel.setText(broker.getFullName());
        UiClock.bind(clockLabel);

        setupMarketTable();
        setupPortfolioTable();
        setupAllActivityTable();
        setupClientHistoryTable();
        setupClientList();
        setupTradePanel();

        try {
            ClientPriceListener listener = new ClientPriceListener(stock -> {
                // Update market table in real time
                for (int i = 0; i < stocks.size(); i++) {
                    if (stocks.get(i).getSymbol().equals(stock.getSymbol())) {
                        stocks.set(i, stock);
                        break;
                    }
                }
                // Refresh selected client portfolio values
                if (selectedClientId >= 0) {
                    try { loadClientData(); } catch (Exception ignored) {}
                }
            });
            SessionContext.setPriceListener(listener);
            service.registerPriceListener(listener);
            refresh();
        } catch (Exception e) {
            StyleManager.setError(tradeMessageLabel, e.getMessage());
        }
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupMarketTable() {
        stockSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        stockNameCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        stockPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockPriceCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(StyleManager.formatCurrency(v.doubleValue()));
                setStyle("-fx-text-fill:#93c5fd;-fx-font-weight:bold;");
            }
        });
        StyleManager.styleTable(marketTable);
        marketTable.setItems(stocks);
        // Click a stock to auto-fill the trade combo
        marketTable.getSelectionModel().selectedItemProperty().addListener((o, old, s) -> {
            if (s != null) tradeSymbolCombo.setValue(s.getSymbol());
        });
    }

    private void setupPortfolioTable() {
        portSymbolCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().symbol));
        portQtyCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().quantity));
        portAvgCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().avgCost));
        portPriceCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().mktPrice));
        portValueCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().value));
        portPnlCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().pnl));

        // Color P&L green/red
        portPnlCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                double d = v.doubleValue();
                setText(StyleManager.formatCurrency(d));
                setStyle(d >= 0
                        ? "-fx-text-fill:#4ade80;-fx-font-weight:bold;"
                        : "-fx-text-fill:#f87171;-fx-font-weight:bold;");
            }
        });
        StyleManager.styleTable(portfolioTable);
        portfolioTable.setItems(portfolioRows);
    }

    private void setupAllActivityTable() {
        txTimeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedTime()));
        txClientCol.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        txTypeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().name()));
        StyleManager.styleTradeSideColumn(txTypeCol);
        txSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        txQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        txPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        txTotalCol.setCellValueFactory(c -> new SimpleStringProperty(
                StyleManager.formatCurrency(c.getValue().getTotal())));

        activityFilterCombo.getItems().addAll("All", "BUY", "SELL");
        activityFilterCombo.setValue("All");
        filteredTx = new FilteredList<>(allTx, t -> true);
        activityFilterCombo.valueProperty().addListener((o, a, b) -> applyActivityFilter());
        txTable.setItems(filteredTx);
        StyleManager.styleTable(txTable);
    }

    private void setupClientHistoryTable() {
        htTimeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedTime()));
        htTypeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().name()));
        StyleManager.styleTradeSideColumn(htTypeCol);
        htSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        htQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        htPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        htTotalCol.setCellValueFactory(c -> new SimpleStringProperty(
                StyleManager.formatCurrency(c.getValue().getTotal())));

        historyFilterCombo.getItems().addAll("All", "BUY", "SELL");
        historyFilterCombo.setValue("All");
        filteredClientTx = new FilteredList<>(clientTx, t -> true);
        historyFilterCombo.valueProperty().addListener((o, a, b) -> applyHistoryFilter());
        historyTable.setItems(filteredClientTx);
        StyleManager.styleTable(historyTable);
    }

    private void setupClientList() {
        filteredClients = new FilteredList<>(allClients, s -> true);
        clientsList.setItems(filteredClients);
        clientSearchField.textProperty().addListener((o, a, b) -> {
            String q = b == null ? "" : b.trim().toLowerCase();
            filteredClients.setPredicate(s -> q.isEmpty() || s.toLowerCase().contains(q));
        });
        clientsList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) {
                selectedClientId = Integer.parseInt(val.split("\\|")[0].trim());
                try { loadClientData(); }
                catch (Exception ex) { StyleManager.setError(tradeMessageLabel, ex.getMessage()); }
            }
        });
    }

    private void setupTradePanel() {
        tradeSymbolCombo.valueProperty().addListener((o, a, b) -> updateEstimate());
        quantityField.textProperty().addListener((o, a, b) -> updateEstimate());
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void refresh() throws Exception {
        stocks.setAll(service.getStocks());
        tradeSymbolCombo.setItems(FXCollections.observableArrayList(
                stocks.stream().map(Stock::getSymbol).toList()));

        allClients.clear();
        for (ClientProfile cp : service.getClientsForBroker(broker.getId())) {
            User u = service.getUserById(cp.getUserId());
            String name = u != null ? u.getFullName() : "Client " + cp.getUserId();
            allClients.add(cp.getUserId() + " | " + name);
        }
        clientCountLabel.setText(String.valueOf(allClients.size()));

        allTx.setAll(service.getBrokerTransactions(broker.getId()));
        long today = allTx.stream()
                .filter(t -> t.getTimestamp() != null
                        && t.getTimestamp().toLocalDate().equals(LocalDate.now()))
                .count();
        tradesTodayLabel.setText(String.valueOf(today));
        applyActivityFilter();
        footerLabel.setText("Managing " + allClients.size() + " clients  |  "
                + allTx.size() + " total transactions");
    }

    private void loadClientData() throws Exception {
        User client = service.getUserById(selectedClientId);
        selectedClientName = client != null ? client.getFullName() : "Client #" + selectedClientId;
        selectedClientLabel.setText(selectedClientName);

        // Show email
        var profile = service.getClientProfile(selectedClientId);
        if (profile != null) {
            clientEmailLabel.setText(profile.getEmail());
        }

        double cash = service.getBalance(selectedClientId);
        clientBalanceLabel.setText(StyleManager.formatCurrency(cash));

        // Portfolio with P&L
        List<PortfolioItem> holdings = service.getPortfolio(selectedClientId);
        portfolioRows.clear();
        double aum = cash;
        for (PortfolioItem item : holdings) {
            double mkt  = ChartHelper.findStock(stocks, item.getSymbol())
                    .map(Stock::getPrice).orElse(item.getAveragePrice());
            double value = item.getQuantity() * mkt;
            double pnl   = value - (item.getQuantity() * item.getAveragePrice());
            aum += value;
            portfolioRows.add(new PortfolioRow(
                    item.getSymbol(), item.getQuantity(),
                    item.getAveragePrice(), mkt, value, pnl));
        }
        clientAumLabel.setText(StyleManager.formatCurrency(aum));

        // Client history tab
        historyClientLabel.setText("— " + selectedClientName);
        clientTx.setAll(service.getTransactions(selectedClientId));
        applyHistoryFilter();

        updateEstimate();
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    private void applyActivityFilter() {
        String side = activityFilterCombo.getValue();
        filteredTx.setPredicate(tx ->
                side == null || "All".equals(side) || tx.getType().name().equals(side));
    }

    private void applyHistoryFilter() {
        String side = historyFilterCombo.getValue();
        filteredClientTx.setPredicate(tx ->
                side == null || "All".equals(side) || tx.getType().name().equals(side));
    }

    // ── Trade estimate ────────────────────────────────────────────────────────

    private void updateEstimate() {
        if (selectedClientId < 0) {
            brokerEstimateLabel.setText("Select a client first.");
            return;
        }
        String sym = tradeSymbolCombo.getValue();
        if (sym == null) { brokerEstimateLabel.setText(""); return; }
        ChartHelper.findStock(stocks, sym).ifPresentOrElse(s -> {
            try {
                int qty = Integer.parseInt(quantityField.getText().trim());
                brokerEstimateLabel.setText(qty + " × "
                        + StyleManager.formatCurrency(s.getPrice())
                        + " = " + StyleManager.formatCurrency(s.getPrice() * qty));
            } catch (Exception e) {
                brokerEstimateLabel.setText("@ " + StyleManager.formatCurrency(s.getPrice()) + " / share");
            }
        }, () -> brokerEstimateLabel.setText(""));
    }

    // ── Trade execution ───────────────────────────────────────────────────────

    @FXML private void onBuy()  { executeTrade(true);  }
    @FXML private void onSell() { executeTrade(false); }

    private void executeTrade(boolean buy) {
        if (selectedClientId < 0) {
            StyleManager.setError(tradeMessageLabel, "Select a client first.");
            return;
        }
        // Require confirmation checkbox
        if (!confirmCheck.isSelected()) {
            StyleManager.setError(tradeMessageLabel,
                    "Check the confirmation box before executing.");
            return;
        }
        String sym = tradeSymbolCombo.getValue();
        if (sym == null || sym.isBlank()) {
            StyleManager.setError(tradeMessageLabel, "Select a stock symbol.");
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            StyleManager.setError(tradeMessageLabel, "Enter a valid positive quantity.");
            return;
        }

        try {
            String result = service.executeTrade(
                    selectedClientId, broker.getId(), sym, qty, buy);
            if ("SUCCESS".equals(result)) {
                StyleManager.setSuccess(tradeMessageLabel,
                        (buy ? "Bought " : "Sold ") + qty + " " + sym
                        + " for " + selectedClientName + ".");
                confirmCheck.setSelected(false);
                quantityField.clear();
                loadClientData();
                allTx.setAll(service.getBrokerTransactions(broker.getId()));
                applyActivityFilter();
            } else {
                StyleManager.setError(tradeMessageLabel, result);
            }
        } catch (Exception e) {
            StyleManager.setError(tradeMessageLabel, e.getMessage());
        }
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    @FXML
    private void onRefresh() {
        try {
            refresh();
            if (selectedClientId >= 0) loadClientData();
        } catch (Exception e) {
            StyleManager.setError(tradeMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onLogout() throws Exception {
        if (SessionContext.getPriceListener() != null) {
            service.unregisterPriceListener(SessionContext.getPriceListener());
        }
        SessionContext.clear();
        SceneRouter.goTo("Login.fxml", "BrokerCraft", 900, 620);
    }

    // ── Portfolio row model ───────────────────────────────────────────────────

    public static class PortfolioRow {
        final String symbol;
        final int    quantity;
        final double avgCost;
        final double mktPrice;
        final double value;
        final double pnl;

        PortfolioRow(String symbol, int qty, double avg, double mkt, double val, double pnl) {
            this.symbol   = symbol;
            this.quantity = qty;
            this.avgCost  = avg;
            this.mktPrice = mkt;
            this.value    = val;
            this.pnl      = pnl;
        }

        public String getSymbol()   { return symbol; }
        public int    getQuantity() { return quantity; }
        public double getAvgCost()  { return avgCost; }
        public double getMktPrice() { return mktPrice; }
        public double getValue()    { return value; }
        public double getPnl()      { return pnl; }
    }
}
