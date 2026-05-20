package brokercraft.controllers;

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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

public class BrokerDashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label clockLabel;
    @FXML private Label clientCountLabel;
    @FXML private Label clientAumLabel;
    @FXML private Label tradesTodayLabel;
    @FXML private Label footerLabel;
    @FXML private TextField clientSearchField;
    @FXML private ListView<String> clientsList;
    @FXML private Label selectedClientLabel;
    @FXML private Label clientBalanceLabel;
    @FXML private Label brokerEstimateLabel;
    @FXML private TableView<Stock> marketTable;
    @FXML private TableColumn<Stock, String> stockSymbolCol;
    @FXML private TableColumn<Stock, String> stockNameCol;
    @FXML private TableColumn<Stock, Number> stockPriceCol;
    @FXML private TableView<PortfolioRow> portfolioTable;
    @FXML private TableColumn<PortfolioRow, String> portSymbolCol;
    @FXML private TableColumn<PortfolioRow, Number> portQtyCol;
    @FXML private TableColumn<PortfolioRow, Number> portValueCol;
    @FXML private TableView<Transaction> txTable;
    @FXML private TableColumn<Transaction, String> txTimeCol;
    @FXML private TableColumn<Transaction, String> txClientCol;
    @FXML private TableColumn<Transaction, String> txTypeCol;
    @FXML private TableColumn<Transaction, String> txSymbolCol;
    @FXML private TableColumn<Transaction, Number> txQtyCol;
    @FXML private ComboBox<String> tradeSymbolCombo;
    @FXML private ComboBox<String> activityFilterCombo;
    @FXML private TextField quantityField;
    @FXML private Label tradeMessageLabel;

    private final ObservableList<Stock> stocks = FXCollections.observableArrayList();
    private final ObservableList<String> allClients = FXCollections.observableArrayList();
    private final ObservableList<PortfolioRow> portfolioRows = FXCollections.observableArrayList();
    private final ObservableList<Transaction> allTx = FXCollections.observableArrayList();
    private FilteredList<String> filteredClients;
    private FilteredList<Transaction> filteredTx;
    private BrokerCraftService service;
    private User broker;
    private int selectedClientId = -1;

    @FXML
    private void initialize() {
        broker = SessionContext.getCurrentUser();
        service = SessionContext.getService();
        welcomeLabel.setText(broker.getFullName());
        UiClock.bind(clockLabel);

        stockSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        stockNameCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        stockPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        StyleManager.styleTable(marketTable);
        marketTable.setItems(stocks);

        portSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        portQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        portValueCol.setCellValueFactory(new PropertyValueFactory<>("marketValue"));
        StyleManager.styleTable(portfolioTable);
        portfolioTable.setItems(portfolioRows);

        txTimeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedTime()));
        txClientCol.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        txTypeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().name()));
        StyleManager.styleTradeSideColumn(txTypeCol);
        txSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        txQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        filteredTx = new FilteredList<>(allTx, t -> true);
        txTable.setItems(filteredTx);
        StyleManager.styleTable(txTable);

        activityFilterCombo.getItems().addAll("All", "BUY", "SELL");
        activityFilterCombo.setValue("All");
        activityFilterCombo.valueProperty().addListener((o, a, b) -> applyTxFilter());

        filteredClients = new FilteredList<>(allClients, s -> true);
        clientsList.setItems(filteredClients);
        clientSearchField.textProperty().addListener((o, a, b) -> {
            String q = b == null ? "" : b.trim().toLowerCase();
            filteredClients.setPredicate(s -> q.isEmpty() || s.toLowerCase().contains(q));
        });

        clientsList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) {
                selectedClientId = Integer.parseInt(val.split("\\|")[0].trim());
                try {
                    loadClientData();
                } catch (Exception ex) {
                    StyleManager.setError(tradeMessageLabel, ex.getMessage());
                }
            }
        });

        tradeSymbolCombo.valueProperty().addListener((o, a, b) -> updateEstimate());
        quantityField.textProperty().addListener((o, a, b) -> updateEstimate());

        try {
            ClientPriceListener listener = new ClientPriceListener(stock -> {
                for (int i = 0; i < stocks.size(); i++) {
                    if (stocks.get(i).getSymbol().equals(stock.getSymbol())) {
                        stocks.set(i, stock);
                        break;
                    }
                }
                if (selectedClientId >= 0) {
                    try {
                        loadClientData();
                    } catch (Exception ignored) {
                        // refresh on tick
                    }
                }
            });
            SessionContext.setPriceListener(listener);
            service.registerPriceListener(listener);
            refresh();
        } catch (Exception e) {
            StyleManager.setError(tradeMessageLabel, e.getMessage());
        }
    }

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
                .filter(t -> t.getTimestamp() != null && t.getTimestamp().toLocalDate().equals(LocalDate.now()))
                .count();
        tradesTodayLabel.setText(String.valueOf(today));
        applyTxFilter();
        footerLabel.setText("Managing " + allClients.size() + " clients");
    }

    private void applyTxFilter() {
        String side = activityFilterCombo.getValue();
        filteredTx.setPredicate(tx -> side == null || "All".equals(side) || tx.getType().name().equals(side));
    }

    private void loadClientData() throws Exception {
        User client = service.getUserById(selectedClientId);
        selectedClientLabel.setText(client != null ? client.getFullName() : "Client #" + selectedClientId);
        double cash = service.getBalance(selectedClientId);
        clientBalanceLabel.setText(StyleManager.formatCurrency(cash));

        List<PortfolioItem> holdings = service.getPortfolio(selectedClientId);
        portfolioRows.clear();
        double aum = cash;
        for (PortfolioItem item : holdings) {
            double mkt = ChartHelper.findStock(stocks, item.getSymbol())
                    .map(Stock::getPrice).orElse(item.getAveragePrice());
            double val = item.getQuantity() * mkt;
            aum += val;
            portfolioRows.add(new PortfolioRow(item.getSymbol(), item.getQuantity(), val));
        }
        clientAumLabel.setText(StyleManager.formatCurrency(aum));
        updateEstimate();
    }

    private void updateEstimate() {
        if (selectedClientId < 0) {
            brokerEstimateLabel.setText("Select a client first.");
            return;
        }
        String sym = tradeSymbolCombo.getValue();
        if (sym == null) return;
        ChartHelper.findStock(stocks, sym).ifPresentOrElse(s -> {
            try {
                int qty = Integer.parseInt(quantityField.getText().trim());
                brokerEstimateLabel.setText("Est. " + StyleManager.formatCurrency(s.getPrice() * qty));
            } catch (Exception e) {
                brokerEstimateLabel.setText("@" + StyleManager.formatCurrency(s.getPrice()) + " / share");
            }
        }, () -> brokerEstimateLabel.setText(""));
    }

    @FXML private void onBuy() { executeTrade(true); }
    @FXML private void onSell() { executeTrade(false); }

    private void executeTrade(boolean buy) {
        if (selectedClientId < 0) {
            StyleManager.setError(tradeMessageLabel, "Select a client.");
            return;
        }
        try {
            int qty = Integer.parseInt(quantityField.getText().trim());
            String result = service.executeTrade(selectedClientId, broker.getId(),
                    tradeSymbolCombo.getValue(), qty, buy);
            if ("SUCCESS".equals(result)) {
                StyleManager.setSuccess(tradeMessageLabel, "Trade executed.");
                loadClientData();
                allTx.setAll(service.getBrokerTransactions(broker.getId()));
                applyTxFilter();
            } else {
                StyleManager.setError(tradeMessageLabel, result);
            }
        } catch (NumberFormatException e) {
            StyleManager.setError(tradeMessageLabel, "Invalid quantity.");
        } catch (Exception e) {
            StyleManager.setError(tradeMessageLabel, e.getMessage());
        }
    }

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
        SceneRouter.goTo("Login.fxml", "BrokerCraft", 1100, 680);
    }

    public static class PortfolioRow {
        private final SimpleStringProperty symbol = new SimpleStringProperty();
        private final SimpleIntegerProperty quantity = new SimpleIntegerProperty();
        private final SimpleDoubleProperty marketValue = new SimpleDoubleProperty();

        public PortfolioRow(String symbol, int qty, double value) {
            this.symbol.set(symbol);
            this.quantity.set(qty);
            this.marketValue.set(value);
        }

        public String getSymbol() { return symbol.get(); }
        public int getQuantity() { return quantity.get(); }
        public double getMarketValue() { return marketValue.get(); }
    }
}
