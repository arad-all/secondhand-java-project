package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * UI logic for the create-advertisement page.
 *
 * Note: category and city are entered as plain ids for milestone 1. Once
 * the backend exposes GET /api/categories and GET /api/cities, these
 * text fields should be replaced with ComboBoxes populated from those
 * endpoints instead of asking the user to type an id.
 */
public class CreateAdvertisementController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField categoryIdField;
    @FXML
    private TextField cityIdField;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void handleSubmit() {
        String title = titleField.getText();
        String description = descriptionField.getText();

        if (title == null || title.isBlank()) {
            showError("Title is required.");
            return;
        }

        BigDecimal price;
        Long categoryId;
        Long cityId;

        try {
            price = new BigDecimal(priceField.getText().trim());
            categoryId = Long.parseLong(categoryIdField.getText().trim());
            cityId = Long.parseLong(cityIdField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Price, category id and city id must be valid numbers.");
            return;
        }

        try {
            JsonNode response = apiClient.createAdvertisement(title, description, price, categoryId, cityId);
            showSuccess("Advertisement submitted: " + response.path("title").asText(title)
                    + " (status: " + response.path("status").asText("PENDING_REVIEW") + ")");
        } catch (IOException e) {
            showError("Could not submit advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showError("Submission was interrupted. Please try again.");
        }
    }

    @FXML
    private void handleCancel() {
        try {
            Main.switchScene("/view/advertisement-list.fxml");
        } catch (IOException e) {
            showError("Could not return to the advertisement list.");
        }
    }

    private void showError(String text) {
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setText(text);
    }

    private void showSuccess(String text) {
        errorLabel.setStyle("-fx-text-fill: green;");
        errorLabel.setText(text);
    }
}
