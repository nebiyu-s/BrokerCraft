package brokercraft.controllers;

import brokercraft.main.SceneRouter;
import brokercraft.main.SessionContext;
import brokercraft.rmi.BrokerCraftService;
import brokercraft.utils.StyleManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label messageLabel;

    @FXML
    private void onSubmit() {
        if (!passwordField.getText().equals(confirmField.getText())) {
            StyleManager.setError(messageLabel, "Passwords do not match.");
            return;
        }
        try {
            BrokerCraftService service = SessionContext.getService();
            service.registerClient(
                    usernameField.getText().trim(),
                    passwordField.getText(),
                    fullNameField.getText().trim(),
                    emailField.getText().trim());
            StyleManager.setSuccess(messageLabel, "Application submitted! You'll be notified after admin approval.");
        } catch (Exception e) {
            StyleManager.setError(messageLabel, e.getMessage());
        }
    }

    @FXML
    private void onBack() throws Exception {
        SceneRouter.goTo("Login.fxml", "BrokerCraft — Login", 960, 640);
    }
}
