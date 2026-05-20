package brokercraft.main;

import brokercraft.network.RMIClient;
import brokercraft.rmi.BrokerCraftService;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        try {
            BrokerCraftService service = RMIClient.connect();
            SessionContext.setService(service);
            SceneRouter.init(stage);
            SceneRouter.goTo("Login.fxml", "BrokerCraft — Login", 480, 560);
            stage.show();
        } catch (Exception e) {
            System.err.println("Cannot connect to server. Start it with: ./quick-run.sh server");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
