package brokercraft.controllers;

import java.util.List;

import brokercraft.main.SceneRouter;
import brokercraft.main.SessionContext;
import brokercraft.model.IpoListing;
import brokercraft.model.User;
import brokercraft.rmi.BrokerCraftService;
import brokercraft.utils.StyleManager;
import brokercraft.utils.UiClock;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * CompanyDashboardController — dashboard for a logged-in COMPANY user.
 *
 * Tab 1 "My IPOs"    — shows all IPOs this company has submitted with their status.
 * Tab 2 "Submit IPO" — form to submit a new IPO for admin approval.
 */
public class CompanyDashboardController {

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label clockLabel;
    @FXML private Label statusBadge;
    @FXML private Label footerLabel;

    // ── Metric cards ──────────────────────────────────────────────────────────
    @FXML private Label totalIposLabel;
    @FXML private Label openIposLabel;
    @FXML private Label sharesSoldLabel;
    @FXML private Label capitalRaisedLabel;

    // ── IPO table ─────────────────────────────────────────────────────────────
    @FXML private TableView<IpoRow>              ipoTable;
    @FXML private TableColumn<IpoRow, String>    ipoSymbolCol;
    @FXML private TableColumn<IpoRow, Number>    ipoOfferedCol;
    @FXML private TableColumn<IpoRow, Number>    ipoRemainingCol;
    @FXML private TableColumn<IpoRow, String>    ipoSoldCol;
    @FXML private TableColumn<IpoRow, Number>    ipoPriceCol;
    @FXML private TableColumn<IpoRow, String>    ipoDeadlineCol;
    @FXML private TableColumn<IpoRow, String>    ipoStatusCol;

    // ── Submit IPO form ───────────────────────────────────────────────────────
    @FXML private TextField symbolField;
    @FXML private TextField sharesField;
    @FXML private TextField priceField;
    @FXML private TextField deadlineField;
    @FXML private TextArea  ipoDescArea;
    @FXML private Label     ipoMessageLabel;

    private final ObservableList<IpoRow> ipoRows = FXCollections.observableArrayList();
    private BrokerCraftService service;
    private User company;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        company = SessionContext.getCurrentUser();
        service = SessionContext.getService();
        welcomeLabel.setText(company.getFullName());
        UiClock.bind(clockLabel);

        // Set up IPO table columns
        ipoSymbolCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().symbol));
        ipoOfferedCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().sharesOffered));
        ipoRemainingCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().sharesRemaining));
        ipoSoldCol.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.1f%%", c.getValue().soldPercent)));
        ipoPriceCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().pricePerShare));
        ipoDeadlineCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().deadline));
        ipoStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));

        // Color-code the status column
        ipoStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "OPEN"     -> "-fx-text-fill:#4ade80;-fx-font-weight:bold;";
                    case "PENDING"  -> "-fx-text-fill:#fbbf24;-fx-font-weight:bold;";
                    case "CLOSED"   -> "-fx-text-fill:#94a3b8;-fx-font-weight:bold;";
                    case "REJECTED" -> "-fx-text-fill:#f87171;-fx-font-weight:bold;";
                    default         -> "";
                });
            }
        });

        StyleManager.styleTable(ipoTable);
        ipoTable.setItems(ipoRows);

        // Check company approval status
        try {
            var profile = service.getCompanyProfile(company.getId());
            if (profile != null) {
                String status = profile.getStatus().name();
                statusBadge.setText("● " + status);
                statusBadge.setStyle(switch (status) {
                    case "APPROVED" -> "-fx-text-fill:#4ade80;-fx-font-weight:bold;";
                    case "PENDING"  -> "-fx-text-fill:#fbbf24;-fx-font-weight:bold;";
                    default         -> "-fx-text-fill:#f87171;-fx-font-weight:bold;";
                });
            }
            refresh();
        } catch (Exception e) {
            StyleManager.setError(ipoMessageLabel, e.getMessage());
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    private void refresh() throws Exception {
        List<IpoListing> ipos = service.getIposForCompany(company.getId());

        ipoRows.clear();
        int openCount   = 0;
        int totalSold   = 0;
        double capital  = 0;

        for (IpoListing ipo : ipos) {
            ipoRows.add(new IpoRow(ipo));
            if (ipo.getStatus() == IpoListing.IpoStatus.OPEN) openCount++;
            totalSold += ipo.getSoldShares();
            capital   += ipo.getSoldShares() * ipo.getPricePerShare();
        }

        totalIposLabel.setText(String.valueOf(ipos.size()));
        openIposLabel.setText(String.valueOf(openCount));
        sharesSoldLabel.setText(String.valueOf(totalSold));
        capitalRaisedLabel.setText(StyleManager.formatCurrency(capital));
        footerLabel.setText("Last updated " + java.time.LocalTime.now().withNano(0));
    }

    // ── Submit IPO ────────────────────────────────────────────────────────────

    @FXML
    private void onSubmitIpo() {
        String symbol      = symbolField.getText().trim().toUpperCase();
        String sharesStr   = sharesField.getText().trim();
        String priceStr    = priceField.getText().trim();
        String deadline    = deadlineField.getText().trim();
        String description = ipoDescArea.getText().trim();

        // Validate inputs
        if (symbol.isEmpty() || sharesStr.isEmpty() || priceStr.isEmpty() || deadline.isEmpty()) {
            StyleManager.setError(ipoMessageLabel, "Symbol, shares, price and deadline are required.");
            return;
        }
        if (!symbol.matches("[A-Z]{1,10}")) {
            StyleManager.setError(ipoMessageLabel, "Symbol must be 1-10 uppercase letters only.");
            return;
        }

        int    shares;
        double price;
        try {
            shares = Integer.parseInt(sharesStr);
            price  = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            StyleManager.setError(ipoMessageLabel, "Shares must be a whole number, price must be a number.");
            return;
        }

        try {
            service.submitIpo(company.getId(), symbol, shares, price, description, deadline);

            // Clear form
            symbolField.clear();
            sharesField.clear();
            priceField.clear();
            deadlineField.clear();
            ipoDescArea.clear();

            StyleManager.setSuccess(ipoMessageLabel,
                    "IPO submitted for admin approval. You'll see it in 'My IPOs' tab.");

            // Auto-clear message after 4 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(4));
            pause.setOnFinished(e -> ipoMessageLabel.setText(""));
            pause.play();

            refresh();

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("nested exception is:")) {
                int idx = msg.lastIndexOf("nested exception is:");
                msg = msg.substring(idx + "nested exception is:".length()).trim();
                if (msg.contains(":")) msg = msg.substring(msg.indexOf(":") + 1).trim();
            }
            showMsg(ipoMessageLabel, msg != null ? msg : "Submission failed.", false);
        }
    }

    @FXML
    private void onRefresh() {
        try { refresh(); }
        catch (Exception e) { StyleManager.setError(ipoMessageLabel, e.getMessage()); }
    }

    private void showMsg(Label label, String text, boolean success) {
        if (success) StyleManager.setSuccess(label, text);
        else         StyleManager.setError(label, text);
    }

    @FXML
    private void onLogout() throws Exception {
        SessionContext.clear();
        SceneRouter.goTo("Login.fxml", "BrokerCraft — Login", 900, 620);
    }

    // ── Inner row class for TableView ─────────────────────────────────────────

    public static class IpoRow {
        final String symbol;
        final int    sharesOffered;
        final int    sharesRemaining;
        final double soldPercent;
        final double pricePerShare;
        final String deadline;
        final String status;

        IpoRow(IpoListing ipo) {
            this.symbol          = ipo.getSymbol();
            this.sharesOffered   = ipo.getSharesOffered();
            this.sharesRemaining = ipo.getSharesRemaining();
            this.soldPercent     = ipo.getSoldPercent();
            this.pricePerShare   = ipo.getPricePerShare();
            this.deadline        = ipo.getDeadline().toString();
            this.status          = ipo.getStatus().name();
        }
    }
}
