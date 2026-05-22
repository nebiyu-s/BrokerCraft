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
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        // Admin is no longer a JavaFX role — removed from the combo
        roleCombo.getItems().addAll("Broker", "Client");
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
            UserRole role = parseRole(roleCombo.getValue());
            User user = service.login(username, password, role);

            if (user == null) {
                StyleManager.setError(errorLabel, "Invalid credentials or account not yet active.");
                return;
            }

            SessionContext.setCurrentUser(user);

            switch (role) {
                case BROKER -> SceneRouter.goTo("BrokerDashboard.fxml", "BrokerCraft — Advisor", 1200, 760);
                case CLIENT -> SceneRouter.goTo("ClientDashboard.fxml", "BrokerCraft — Investor", 1200, 760);
                default     -> StyleManager.setError(errorLabel, "Unknown role.");
            }

        } catch (Exception e) {
            StyleManager.setError(errorLabel, "Login failed: " + e.getMessage());
        }
    }

    @FXML
    private void onRegister() throws Exception {
        SceneRouter.goTo("Register.fxml", "BrokerCraft — Register", 1100, 680);
    }

    /**
     * Opens the Admin web dashboard in the system default browser.
     * Called when the user clicks "Admin Dashboard" link on the login screen.
     */
    @FXML
    private void onOpenAdminDashboard() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:7000/admin"));
            } else {
                StyleManager.setError(errorLabel,
                        "Cannot open browser automatically. Go to: http://localhost:7000/admin");
            }
        } catch (Exception e) {
            StyleManager.setError(errorLabel, "Could not open browser: " + e.getMessage());
        }
    }

    private UserRole parseRole(String label) {
        return switch (label) {
            case "Broker" -> UserRole.BROKER;
            default       -> UserRole.CLIENT;
        };
    }
}
