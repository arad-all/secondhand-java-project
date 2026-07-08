package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * UI logic for the registration page.
 */
public class RegisterController {

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private TextField emailField;
    @FXML
    private Label messageLabel;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void handleRegister() {
        String fullName = fullNameField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String phoneNumber = phoneNumberField.getText();
        String email = emailField.getText();

        if (isBlank(fullName) || isBlank(username) || isBlank(password) || isBlank(phoneNumber)) {
            showError("Full name, username, password and phone number are required.");
            return;
        }

        try {
            JsonNode response = apiClient.register(fullName, username, password, phoneNumber, email);
            showSuccess(response.path("message").asText("Registration successful."));
        } catch (IOException e) {
            showError("Registration failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showError("Registration was interrupted. Please try again.");
        }
    }

    @FXML
    private void handleGoToLogin() {
        try {
            Main.switchScene("/view/login.fxml");
        } catch (IOException e) {
            showError("Could not open the login page.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void showError(String text) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(text);
    }

    private void showSuccess(String text) {
        messageLabel.setStyle("-fx-text-fill: green;");
        messageLabel.setText(text);
    }
}
