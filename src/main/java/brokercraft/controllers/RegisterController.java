package brokercraft.controllers;

import brokercraft.main.SceneRouter;
import brokercraft.main.SessionContext;
import brokercraft.rmi.BrokerCraftService;
import brokercraft.utils.StyleManager;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         messageLabel;

    @FXML
    private void onSubmit() {
        // Basic validation
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            StyleManager.setError(messageLabel, "All fields are required.");
            return;
        }
        if (!password.equals(confirm)) {
            StyleManager.setError(messageLabel, "Passwords do not match.");
            return;
        }

        try {
            BrokerCraftService service = SessionContext.getService();
            service.registerClient(username, password, fullName, email);

            // ── Success: clear all fields immediately ──
            fullNameField.clear();
            usernameField.clear();
            emailField.clear();
            passwordField.clear();
            confirmField.clear();

            // Show success message
            StyleManager.setSuccess(messageLabel,
                    "Application submitted! Wait for admin approval before logging in.");

            // Auto-clear the success message after 4 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(4));
            pause.setOnFinished(e -> messageLabel.setText(""));
            pause.play();

        } catch (Exception e) {
            StyleManager.setError(messageLabel, e.getMessage());
        }
    }

    @FXML
    private void onBack() throws Exception {
        SceneRouter.goTo("Login.fxml", "BrokerCraft — Login", 900, 620);
    }
}
