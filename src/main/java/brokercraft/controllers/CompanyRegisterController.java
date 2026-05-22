package brokercraft.controllers;

import brokercraft.main.SceneRouter;
import brokercraft.main.SessionContext;
import brokercraft.rmi.BrokerCraftService;
import brokercraft.utils.StyleManager;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class CompanyRegisterController {

    @FXML private TextField     companyNameField;
    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;
    @FXML private ComboBox<String> industryCombo;
    @FXML private TextArea      descriptionArea;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         messageLabel;

    @FXML
    private void initialize() {
        industryCombo.getItems().addAll(
                "Banking", "Insurance", "Manufacturing",
                "Technology", "Agriculture", "Energy",
                "Retail", "Healthcare", "Real Estate", "Other");
        industryCombo.setValue("Banking");
    }

    @FXML
    private void onSubmit() {
        String companyName  = companyNameField.getText().trim();
        String username     = usernameField.getText().trim();
        String email        = emailField.getText().trim();
        String industry     = industryCombo.getValue();
        String description  = descriptionArea.getText().trim();
        String password     = passwordField.getText();
        String confirm      = confirmField.getText();

        // Validate
        if (companyName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMsg(messageLabel, "Company name, username, email and password are required.", false);
            return;
        }
        if (!password.equals(confirm)) {
            showMsg(messageLabel, "Passwords do not match.", false);
            return;
        }

        try {
            BrokerCraftService service = SessionContext.getService();
            service.registerCompany(username, password, companyName, email, description, industry);

            // Clear all fields
            companyNameField.clear();
            usernameField.clear();
            emailField.clear();
            descriptionArea.clear();
            passwordField.clear();
            confirmField.clear();
            industryCombo.setValue("Banking");

            showMsg(messageLabel,
                    "Application submitted! Admin will review and approve your company.", true);

        } catch (Exception e) {
            // Show a clean error message — strip the RMI wrapper noise
            String msg = e.getMessage();
            if (msg != null && msg.contains("nested exception is:")) {
                // Extract the root cause message
                int idx = msg.lastIndexOf("nested exception is:");
                msg = msg.substring(idx + "nested exception is:".length()).trim();
                // Further clean up if it starts with class name
                if (msg.contains(":")) {
                    msg = msg.substring(msg.indexOf(":") + 1).trim();
                }
            }
            showMsg(messageLabel, msg != null ? msg : "Registration failed.", false);
        }
    }

    /**
     * Show a message and auto-clear it after 4 seconds.
     * Both success and error messages clear automatically.
     */
    private void showMsg(Label label, String text, boolean success) {
        if (success) {
            StyleManager.setSuccess(label, text);
        } else {
            StyleManager.setError(label, text);
        }
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> label.setText(""));
        pause.play();
    }

    @FXML
    private void onBack() throws Exception {
        SceneRouter.goTo("Login.fxml", "BrokerCraft — Login", 900, 620);
    }
}
