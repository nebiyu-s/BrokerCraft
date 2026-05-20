package brokercraft.controllers;

import brokercraft.main.SceneRouter;
import brokercraft.main.SessionContext;
import brokercraft.model.ClientProfile;
import brokercraft.model.Stock;
import brokercraft.model.Transaction;
import brokercraft.model.User;
import brokercraft.network.ClientPriceListener;
import brokercraft.rmi.BrokerCraftService;
import brokercraft.utils.StyleManager;
import brokercraft.utils.UiClock;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class AdminDashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label clockLabel;
    @FXML private Label simStatusLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label brokerCountLabel;
    @FXML private Label txCountLabel;
    @FXML private Label sessionCountLabel;
    @FXML private Label stockCountLabel;
    @FXML private Label footerLabel;
    @FXML private TextField pendingSearchField;
    @FXML private TableView<Stock> marketTable;
    @FXML private TableColumn<Stock, String> stockSymbolCol;
    @FXML private TableColumn<Stock, String> stockNameCol;
    @FXML private TableColumn<Stock, Number> stockPriceCol;
    @FXML private TableView<PendingRow> pendingTable;
    @FXML private TableColumn<PendingRow, String> pendingNameCol;
    @FXML private TableColumn<PendingRow, String> pendingUserCol;
    @FXML private TableColumn<PendingRow, String> pendingEmailCol;
    @FXML private TableView<BrokerRow> brokersTable;
    @FXML private TableColumn<BrokerRow, String> brokerNameCol;
    @FXML private TableColumn<BrokerRow, String> brokerUserCol;
    @FXML private TableColumn<BrokerRow, String> brokerDeptCol;
    @FXML private TableColumn<BrokerRow, Number> brokerClientsCol;
    @FXML private ComboBox<String> brokerAssignCombo;
    @FXML private TableView<Transaction> txTable;
    @FXML private TableColumn<Transaction, String> txTimeCol;
    @FXML private TableColumn<Transaction, String> txClientCol;
    @FXML private TableColumn<Transaction, String> txTypeCol;
    @FXML private TableColumn<Transaction, String> txSymbolCol;
    @FXML private TableColumn<Transaction, Number> txQtyCol;
    @FXML private TextField brokerUsernameField;
    @FXML private PasswordField brokerPasswordField;
    @FXML private TextField brokerNameField;
    @FXML private TextField brokerDeptField;
    @FXML private Label adminMessageLabel;
    @FXML private ListView<String> activeUsersList;

    private final ObservableList<Stock> stocks = FXCollections.observableArrayList();
    private final ObservableList<PendingRow> pending = FXCollections.observableArrayList();
    private final ObservableList<BrokerRow> brokers = FXCollections.observableArrayList();
    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private FilteredList<PendingRow> filteredPending;
    private BrokerCraftService service;
    private User admin;

    @FXML
    private void initialize() {
        admin = SessionContext.getCurrentUser();
        service = SessionContext.getService();
        welcomeLabel.setText(admin.getFullName());
        UiClock.bind(clockLabel);

        stockSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        stockNameCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        stockPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        StyleManager.styleTable(marketTable);
        marketTable.setItems(stocks);

        pendingNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        pendingUserCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        pendingEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        filteredPending = new FilteredList<>(pending, p -> true);
        pendingTable.setItems(filteredPending);
        StyleManager.styleTable(pendingTable);
        pendingSearchField.textProperty().addListener((o, a, b) -> {
            String q = b == null ? "" : b.trim().toLowerCase();
            filteredPending.setPredicate(row ->
                    q.isEmpty()
                            || row.getFullName().toLowerCase().contains(q)
                            || row.getUsername().toLowerCase().contains(q)
                            || row.getEmail().toLowerCase().contains(q));
        });

        brokerNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        brokerUserCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        brokerDeptCol.setCellValueFactory(new PropertyValueFactory<>("department"));
        brokerClientsCol.setCellValueFactory(new PropertyValueFactory<>("clientCount"));
        StyleManager.styleTable(brokersTable);
        brokersTable.setItems(brokers);

        txTimeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedTime()));
        txClientCol.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        txTypeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().name()));
        StyleManager.styleTradeSideColumn(txTypeCol);
        txSymbolCol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        txQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        StyleManager.styleTable(txTable);
        txTable.setItems(transactions);

        try {
            ClientPriceListener listener = new ClientPriceListener(stock -> {
                for (int i = 0; i < stocks.size(); i++) {
                    if (stocks.get(i).getSymbol().equals(stock.getSymbol())) {
                        stocks.set(i, stock);
                        break;
                    }
                }
            });
            SessionContext.setPriceListener(listener);
            service.registerPriceListener(listener);
            refresh();
        } catch (Exception e) {
            StyleManager.setError(adminMessageLabel, e.getMessage());
        }
    }

    private void refresh() throws Exception {
        stocks.setAll(service.getStocks());
        stockCountLabel.setText(String.valueOf(stocks.size()));

        pending.clear();
        for (ClientProfile cp : service.getPendingClients()) {
            User u = service.getUserById(cp.getUserId());
            pending.add(new PendingRow(
                    u != null ? u.getFullName() : "?",
                    u != null ? u.getUsername() : "?",
                    cp.getEmail(),
                    cp.getUserId()));
        }
        pendingCountLabel.setText(String.valueOf(pending.size()));

        brokers.clear();
        List<User> brokerUsers = service.getBrokers();
        for (User b : brokerUsers) {
            int clients = service.getClientsForBroker(b.getId()).size();
            String dept = "General";
            brokers.add(new BrokerRow(b.getFullName(), b.getUsername(), dept, clients));
        }
        brokerCountLabel.setText(String.valueOf(brokerUsers.size()));

        brokerAssignCombo.setItems(FXCollections.observableArrayList(
                brokerUsers.stream().map(User::getUsername).toList()));

        transactions.setAll(service.getAllTransactions());
        txCountLabel.setText(String.valueOf(transactions.size()));

        List<String> sessions = service.getActiveUsernames();
        activeUsersList.setItems(FXCollections.observableArrayList(sessions));
        sessionCountLabel.setText(String.valueOf(sessions.size()));

        StyleManager.updateSimBadge(simStatusLabel, service.isPriceSimulationRunning());
        footerLabel.setText("Last updated " + java.time.LocalTime.now().withNano(0));
    }

    @FXML
    private void onCreateBroker() {
        try {
            service.createBroker(
                    brokerUsernameField.getText().trim(),
                    brokerPasswordField.getText(),
                    brokerNameField.getText().trim(),
                    brokerDeptField.getText().trim());
            StyleManager.setSuccess(adminMessageLabel, "Broker account created.");
            brokerUsernameField.clear();
            brokerPasswordField.clear();
            brokerNameField.clear();
            brokerDeptField.clear();
            refresh();
        } catch (Exception e) {
            StyleManager.setError(adminMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onApprove() {
        PendingRow row = pendingTable.getSelectionModel().getSelectedItem();
        String brokerUsername = brokerAssignCombo.getValue();
        if (row == null || brokerUsername == null) {
            StyleManager.setError(adminMessageLabel, "Select a client and broker.");
            return;
        }
        try {
            User broker = service.getBrokers().stream()
                    .filter(b -> b.getUsername().equals(brokerUsername))
                    .findFirst()
                    .orElseThrow();
            service.approveClient(row.getClientId(), broker.getId());
            StyleManager.setSuccess(adminMessageLabel, "Client approved.");
            refresh();
        } catch (Exception e) {
            StyleManager.setError(adminMessageLabel, e.getMessage());
        }
    }

    @FXML
    private void onReject() {
        PendingRow row = pendingTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            StyleManager.setError(adminMessageLabel, "Select a pending client.");
            return;
        }
        try {
            service.rejectClient(row.getClientId());
            StyleManager.setInfo(adminMessageLabel, "Registration rejected.");
            refresh();
        } catch (Exception e) {
            StyleManager.setError(adminMessageLabel, e.getMessage());
        }
    }

    @FXML private void onStartSim() {
        try { service.startPriceSimulation(); refresh(); }
        catch (Exception e) { StyleManager.setError(adminMessageLabel, e.getMessage()); }
    }

    @FXML private void onStopSim() {
        try { service.stopPriceSimulation(); refresh(); }
        catch (Exception e) { StyleManager.setError(adminMessageLabel, e.getMessage()); }
    }

    @FXML private void onRefresh() {
        try { refresh(); }
        catch (Exception e) { StyleManager.setError(adminMessageLabel, e.getMessage()); }
    }

    @FXML
    private void onLogout() throws Exception {
        if (SessionContext.getPriceListener() != null) {
            service.unregisterPriceListener(SessionContext.getPriceListener());
        }
        SessionContext.clear();
        SceneRouter.goTo("Login.fxml", "BrokerCraft", 1100, 680);
    }

    public static class PendingRow {
        private final SimpleStringProperty fullName = new SimpleStringProperty();
        private final SimpleStringProperty username = new SimpleStringProperty();
        private final SimpleStringProperty email = new SimpleStringProperty();
        private final int clientId;

        public PendingRow(String fullName, String username, String email, int clientId) {
            this.fullName.set(fullName);
            this.username.set(username);
            this.email.set(email);
            this.clientId = clientId;
        }

        public String getFullName() { return fullName.get(); }
        public String getUsername() { return username.get(); }
        public String getEmail() { return email.get(); }
        public int getClientId() { return clientId; }
    }

    public static class BrokerRow {
        private final SimpleStringProperty fullName = new SimpleStringProperty();
        private final SimpleStringProperty username = new SimpleStringProperty();
        private final SimpleStringProperty department = new SimpleStringProperty();
        private final SimpleIntegerProperty clientCount = new SimpleIntegerProperty();

        public BrokerRow(String name, String user, String dept, int clients) {
            fullName.set(name);
            username.set(user);
            department.set(dept);
            clientCount.set(clients);
        }

        public String getFullName() { return fullName.get(); }
        public String getUsername() { return username.get(); }
        public String getDepartment() { return department.get(); }
        public int getClientCount() { return clientCount.get(); }
    }
}
