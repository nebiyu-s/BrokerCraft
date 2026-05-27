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

            // Guard: if service is null the app was opened without a server connection
            if (service == null) {
                showMsg(messageLabel,
                        "Not connected to server. Please restart the app after starting the server.", false);
                return;
            }

            service.registerClient(username, password, fullName, email);

            // ── Success: clear all fields immediately ──
            fullNameField.clear();
            usernameField.clear();
            emailField.clear();
            passwordField.clear();
            confirmField.clear();

            showMsg(messageLabel,
                    "Application submitted! Wait for admin approval before logging in.", true);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("nested exception is:")) {
                int idx = msg.lastIndexOf("nested exception is:");
                msg = msg.substring(idx + "nested exception is:".length()).trim();
                if (msg.contains(":")) msg = msg.substring(msg.indexOf(":") + 1).trim();
            }
            showMsg(messageLabel, msg != null ? msg : "Registration failed.", false);
        }
    }

    private void showMsg(Label label, String text, boolean success) {
        if (success) StyleManager.setSuccess(label, text);
        else         StyleManager.setError(label, text);
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> label.setText(""));
        pause.play();
    }

    @FXML
    private void onBack() throws Exception {
        SceneRouter.goTo("Login.fxml", "BrokerCraft — Login", 900, 620);
    }
}
