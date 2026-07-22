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
 * UI logic for editing an existing advertisement (PATCH /api/advertisements/{id}).
 * The advertisement id to edit is passed in via a static setter called by
 * the details page just before navigating here, same pattern
 * {@code AdvertisementDetailsController} uses.
 */
public class EditAdvertisementController {

    private static Long advertisementIdToEdit;

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
    private final List<String> categoryPlainNames = new ArrayList<>();
    private final List<Long> cityIds = new ArrayList<>();

    public static void setAdvertisementIdToEdit(Long id) {
        advertisementIdToEdit = id;
    }

    @FXML
    private void initialize() {
        loadCategoryOptions();
        loadCityOptions();
        loadCurrentAdvertisement();
    }

    private void loadCategoryOptions() {
        ObservableList<String> options = FXCollections.observableArrayList();
        try {
            for (JsonNode category : apiClient.getCategoriesFlattened()) {
                int depth = category.path("depth").asInt(0);
                String name = category.path("name").asText("");
                options.add(categoryDisplayName(name, depth));
                categoryIds.add(category.path("id").asLong());
                categoryPlainNames.add(name);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            errorLabel.setText("Could not load categories: " + e.getMessage());
        }
        categoryComboBox.setItems(options);
    }

    /** Indents a category name by its depth in the tree (0 = top-level), to any depth — no artificial limit. */
    private String categoryDisplayName(String name, int depth) {
        return "  ".repeat(depth) + (depth > 0 ? "\u2014 " : "") + name;
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
            errorLabel.setText("Could not load cities: " + e.getMessage());
        }
        cityComboBox.setItems(options);
    }

    private void loadCurrentAdvertisement() {
        if (advertisementIdToEdit == null) {
            errorLabel.setText("No advertisement selected to edit.");
            return;
        }

        try {
            JsonNode ad = apiClient.getAdvertisementById(advertisementIdToEdit);
            titleField.setText(ad.path("title").asText(""));
            descriptionField.setText(ad.path("description").asText(""));
            priceField.setText(ad.path("price").asText(""));

            selectCategoryByName(ad.path("categoryName").asText(""));
            selectByName(cityComboBox, ad.path("cityName").asText(""));
        } catch (IOException e) {
            errorLabel.setText("Could not load advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading advertisement was interrupted.");
        }
    }

    /**
     * Selects the category combo box entry with this exact (plain, unindented)
     * name, looked up via the parallel {@link #categoryPlainNames} list rather
     * than pattern-matching the indented display text — works no matter how
     * deep the category is nested.
     */
    private void selectCategoryByName(String name) {
        int index = categoryPlainNames.indexOf(name);
        if (index >= 0 && index < categoryComboBox.getItems().size()) {
            categoryComboBox.getSelectionModel().select(index);
        }
    }

    /** Selects the combo box entry with exactly this visible text — used for city, which isn't indented. */
    private void selectByName(ComboBox<String> comboBox, String name) {
        for (String item : comboBox.getItems()) {
            if (item.equals(name)) {
                comboBox.getSelectionModel().select(item);
                return;
            }
        }
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText();
        if (title != null && title.isBlank()) {
            showError("Title cannot be blank.");
            return;
        }

        BigDecimal price = null;
        String priceText = priceField.getText();
        if (priceText != null && !priceText.isBlank()) {
            try {
                price = new BigDecimal(priceText.trim());
            } catch (NumberFormatException e) {
                showError("Price must be a valid number.");
                return;
            }
        }

        Long categoryId = selectedId(categoryComboBox, categoryIds);
        Long cityId = selectedId(cityComboBox, cityIds);

        try {
            apiClient.editAdvertisement(advertisementIdToEdit, title, descriptionField.getText(), price, categoryId, cityId);
            AdvertisementDetailsController.setSelectedAdvertisementId(advertisementIdToEdit);
            Main.switchScene("/view/advertisement-details.fxml");
        } catch (IOException e) {
            showError("Could not save changes: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showError("Saving was interrupted. Please try again.");
        }
    }

    private Long selectedId(ComboBox<String> comboBox, List<Long> ids) {
        int index = comboBox.getSelectionModel().getSelectedIndex();
        return (index >= 0 && index < ids.size()) ? ids.get(index) : null;
    }

    @FXML
    private void handleCancel() {
        AdvertisementDetailsController.setSelectedAdvertisementId(advertisementIdToEdit);
        try {
            Main.switchScene("/view/advertisement-details.fxml");
        } catch (IOException e) {
            showError("Could not return to the advertisement details.");
        }
    }

    private void showError(String text) {
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setText(text);
    }
}
