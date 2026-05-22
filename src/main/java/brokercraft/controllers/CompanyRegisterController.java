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
            StyleManager.setError(messageLabel, "Company name, username, email and password are required.");
            return;
        }
        if (!password.equals(confirm)) {
            StyleManager.setError(messageLabel, "Passwords do not match.");
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

            StyleManager.setSuccess(messageLabel,
                    "Application submitted! Admin will review and approve your company.");

            // Auto-clear message after 4 seconds
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
