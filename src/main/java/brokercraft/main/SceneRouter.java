package brokercraft.main;

import java.io.IOException;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SceneRouter {
    private static Stage primaryStage;

    // Dashboards that should open maximized
    private static final java.util.Set<String> DASHBOARD_VIEWS = java.util.Set.of(
            "AdminDashboard.fxml", "BrokerDashboard.fxml", "ClientDashboard.fxml"
    );

    private SceneRouter() {}

    public static void init(Stage stage) {
        primaryStage = stage;
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setResizable(true);
    }

    private static void addStylesheet(Scene scene, String resource) {
        String css = Objects.requireNonNull(SceneRouter.class.getResource(resource)).toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }

    public static void goTo(String fxml, String title, int width, int height) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(SceneRouter.class.getResource("/view/" + fxml)));
        Parent root = loader.load();

        boolean isDashboard = DASHBOARD_VIEWS.contains(fxml);

        // Use screen size for dashboards, fixed size for login/register
        Scene scene;
        if (isDashboard) {
            javafx.geometry.Rectangle2D screen =
                    javafx.stage.Screen.getPrimary().getVisualBounds();
            scene = new Scene(root, screen.getWidth(), screen.getHeight());
        } else {
            scene = new Scene(root, width, height);
        }

        addStylesheet(scene, "/styles/main.css");
        addStylesheet(scene, "/styles/components.css");
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);

        if (isDashboard) {
            primaryStage.setMaximized(true);
        } else {
            primaryStage.setMaximized(false);
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.centerOnScreen();
        }
    }
}
