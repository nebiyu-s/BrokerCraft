package brokercraft.main;

import brokercraft.network.RMIClient;
import brokercraft.rmi.BrokerCraftService;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        SceneRouter.init(stage);

        try {
            BrokerCraftService service = RMIClient.connect();
            SessionContext.setService(service);
            SceneRouter.goTo("Login.fxml", "BrokerCraft — Login", 900, 620);
            stage.show();
        } catch (Exception e) {
            // Show a clear error dialog instead of silently opening with null service
            stage.show();
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Cannot Connect to Server");
            alert.setHeaderText("BrokerCraft server is not running");
            alert.setContentText(
                    "Start the server first:\n\n" +
                    "  ./gradlew runServer\n\n" +
                    "Then restart this application.\n\n" +
                    "Details: " + e.getMessage());
            alert.showAndWait();
            // Still load the login screen so the user sees the app
            // but all actions will show "not connected" messages
            try {
                SceneRouter.goTo("Login.fxml", "BrokerCraft — Login (Offline)", 900, 620);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // Entry point for the JavaFX client application.
        // Launches the JavaFX runtime, initializes the UI, and
        // attempts to connect to the BrokerCraft RMI server.
        launch(args);
    }
}
