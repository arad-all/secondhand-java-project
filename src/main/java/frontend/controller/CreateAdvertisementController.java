package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * UI logic for the create-advertisement page. Category and city are
 * chosen from ComboBoxes populated from GET /api/categories(/{id}/children)
 * and GET /api/cities, rather than asking the user to type a raw id.
 */
public class CreateAdvertisementController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField priceField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private ComboBox<String> cityComboBox;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> cityIds = new ArrayList<>();

    @FXML
    private void initialize() {
        loadCategoryOptions();
        loadCityOptions();
    }

    private void loadCategoryOptions() {
        ObservableList<String> options = FXCollections.observableArrayList();
        try {
            for (JsonNode category : apiClient.getCategoriesFlattened()) {
                boolean isSubcategory = category.hasNonNull("parentId");
                options.add((isSubcategory ? "\u2014 " : "") + category.path("name").asText(""));
                categoryIds.add(category.path("id").asLong());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            showError("Could not load categories: " + e.getMessage());
        }
        categoryComboBox.setItems(options);
    }

    private void loadCityOptions() {
        ObservableList<String> options = FXCollections.observableArrayList();
        try {
            for (JsonNode city : apiClient.getCities()) {
                options.add(city.path("name").asText(""));
                cityIds.add(city.path("id").asLong());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            showError("Could not load cities: " + e.getMessage());
        }
        cityComboBox.setItems(options);
    }

    @FXML
    private void handleSubmit() {
        String title = titleField.getText();
        String description = descriptionField.getText();

        if (title == null || title.isBlank()) {
            showError("Title is required.");
            return;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Price must be a valid number.");
            return;
        }

        Long categoryId = selectedId(categoryComboBox, categoryIds);
        Long cityId = selectedId(cityComboBox, cityIds);
        if (categoryId == null || cityId == null) {
            showError("Please choose a category and a city.");
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

    private Long selectedId(ComboBox<String> comboBox, List<Long> ids) {
        int index = comboBox.getSelectionModel().getSelectedIndex();
        return (index >= 0 && index < ids.size()) ? ids.get(index) : null;
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
