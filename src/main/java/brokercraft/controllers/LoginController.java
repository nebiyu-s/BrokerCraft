package brokercraft.controllers;

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
        roleCombo.getItems().addAll("Admin", "Broker", "Client");
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
                case ADMIN -> SceneRouter.goTo("AdminDashboard.fxml", "BrokerCraft — Admin", 1560, 940);
                case BROKER -> SceneRouter.goTo("BrokerDashboard.fxml", "BrokerCraft — Advisor", 1520, 920);
                case CLIENT -> SceneRouter.goTo("ClientDashboard.fxml", "BrokerCraft — Investor", 1520, 920);
            }
        } catch (Exception e) {
            StyleManager.setError(errorLabel, "Login failed: " + e.getMessage());
        }
    }

    @FXML
    private void onRegister() throws Exception {
        SceneRouter.goTo("Register.fxml", "BrokerCraft — Register", 1100, 680);
    }

    private UserRole parseRole(String label) {
        return switch (label) {
            case "Admin" -> UserRole.ADMIN;
            case "Broker" -> UserRole.BROKER;
            default -> UserRole.CLIENT;
        };
    }
}
