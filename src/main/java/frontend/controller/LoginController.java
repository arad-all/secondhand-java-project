package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import frontend.service.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * UI logic for the login page. All HTTP calls go through ApiClient;
 * this class only deals with reading fields, showing errors and
 * navigating to the next page.
 */
public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            errorLabel.setText("Username and password are required.");
            return;
        }

        try {
            JsonNode response = apiClient.login(username, password);

            String token = response.path("token").asText(null);
            Long userId = response.has("userId") ? response.get("userId").asLong() : null;
            String role = response.path("role").asText("USER");

            SessionManager.getInstance().startSession(token, userId, username, role);

            Main.switchScene("/view/advertisement-list.fxml");
        } catch (IOException e) {
            errorLabel.setText("Login failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Login was interrupted. Please try again.");
        }
    }

    @FXML
    private void handleGoToRegister() {
        try {
            Main.switchScene("/view/register.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open the registration page.");
        }
    }
}
