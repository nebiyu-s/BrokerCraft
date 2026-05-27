package brokercraft.controllers;

import java.awt.Desktop;
import java.net.URI;

import brokercraft.main.SceneRouter;
import brokercraft.main.SessionContext;
import brokercraft.model.User;
import brokercraft.model.UserRole;
import brokercraft.rmi.BrokerCraftService;
import brokercraft.utils.StyleManager;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField        usernameField;
    @FXML private PasswordField    passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label            errorLabel;

    @FXML
    private void initialize() {
        // Admin is handled via the web dashboard — not in this combo
        roleCombo.getItems().addAll("Broker", "Client", "Company");
        roleCombo.setValue("Client");
    }

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            StyleManager.setError(errorLabel, "Please enter username and password.");
            return;
        }

        try {
            BrokerCraftService service = SessionContext.getService();

            if (service == null) {
                StyleManager.setError(errorLabel,
                        "Not connected to server. Start the server and restart the app.");
                return;
            }

            UserRole role = parseRole(roleCombo.getValue());
            User user = service.login(username, password, role);

            if (user == null) {
                StyleManager.setError(errorLabel,
                        "Invalid credentials or account not yet active.");
                return;
            }

            SessionContext.setCurrentUser(user);

            switch (role) {
                case BROKER  -> SceneRouter.goTo("BrokerDashboard.fxml",
                        "BrokerCraft — Advisor",  1200, 760);
                case CLIENT  -> SceneRouter.goTo("ClientDashboard.fxml",
                        "BrokerCraft — Investor", 1200, 760);
                case COMPANY -> SceneRouter.goTo("CompanyDashboard.fxml",
                        "BrokerCraft — Company Portal", 1100, 720);
                default      -> StyleManager.setError(errorLabel, "Unknown role.");
            }

        } catch (Exception e) {
            StyleManager.setError(errorLabel, "Login failed: " + e.getMessage());
        }
    }

    /** Open client registration screen */
    @FXML
    private void onRegister() throws Exception {
        SceneRouter.goTo("Register.fxml", "BrokerCraft — Client Registration", 900, 620);
    }

    /** Open company registration screen */
    @FXML
    private void onCompanyRegister() throws Exception {
        SceneRouter.goTo("CompanyRegister.fxml", "BrokerCraft — Company Registration", 900, 680);
    }

    /** Open admin web dashboard in browser */
    @FXML
    private void onOpenAdminDashboard() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:7000/admin"));
            } else {
                StyleManager.setError(errorLabel,
                        "Cannot open browser. Go to: http://localhost:7000/admin");
            }
        } catch (Exception e) {
            StyleManager.setError(errorLabel, "Could not open browser: " + e.getMessage());
        }
    }

    private UserRole parseRole(String label) {
        return switch (label) {
            case "Broker"  -> UserRole.BROKER;
            case "Company" -> UserRole.COMPANY;
            default        -> UserRole.CLIENT;
        };
    }
}
