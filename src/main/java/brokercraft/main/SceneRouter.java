package brokercraft.main;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class SceneRouter {
    private static Stage primaryStage;

    private SceneRouter() {}

    public static void init(Stage stage) {
        primaryStage = stage;
        stage.setMinWidth(900);
        stage.setMinHeight(600);
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
        Scene scene = new Scene(root, width, height);
        addStylesheet(scene, "/styles/main.css");
        addStylesheet(scene, "/styles/components.css");
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.centerOnScreen();
    }
}
