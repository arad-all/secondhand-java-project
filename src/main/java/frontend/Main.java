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

    private static final String STYLESHEET = "/view/app.css";

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("Second-Hand Marketplace");
        primaryStage.setWidth(1150);
        primaryStage.setHeight(760);
        switchScene("/view/login.fxml");
        primaryStage.show();
    }

    /**
     * Loads the given FXML file and sets it as the current scene. The app's
     * single small stylesheet ({@link #STYLESHEET}) is attached once, on
     * the first scene created, and then just stays attached to that same
     * Scene object as its root keeps getting swapped out.
     *
     * @param fxmlPath classpath-relative path, e.g. "/view/login.fxml"
     */
    public static void switchScene(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Parent root = loader.load();

        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Main.class.getResource(STYLESHEET).toExternalForm());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(root);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
