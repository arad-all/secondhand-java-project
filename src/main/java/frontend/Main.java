package frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX application entry point. Also provides a tiny navigation helper
 * (switchScene) so controllers can move between pages without needing a
 * separate navigation framework.
 */
public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("Second-Hand Marketplace");
        switchScene("/view/login.fxml");
        primaryStage.show();
    }

    /**
     * Loads the given FXML file and sets it as the current scene.
     *
     * @param fxmlPath classpath-relative path, e.g. "/view/login.fxml"
     */
    public static void switchScene(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Parent root = loader.load();

        if (primaryStage.getScene() == null) {
            primaryStage.setScene(new Scene(root));
        } else {
            primaryStage.getScene().setRoot(root);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
