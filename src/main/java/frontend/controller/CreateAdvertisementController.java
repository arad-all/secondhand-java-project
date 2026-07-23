package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * UI logic for the create-advertisement page. Category and city are
 * chosen from ComboBoxes populated from GET /api/categories(/{id}/children)
 * and GET /api/cities, rather than asking the user to type a raw id.
 * <p>
 * Images are picked from the local filesystem via {@link FileChooser},
 * kept in memory ({@link #selectedImages}) so they can be removed before
 * submitting, and uploaded (POST /api/advertisements/{id}/images) only
 * after the advertisement itself has been created successfully.
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
    private FlowPane imagePreviewPane;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> cityIds = new ArrayList<>();
    private final List<File> selectedImages = new ArrayList<>();

    @FXML
    private void initialize() {
        loadCategoryOptions();
        loadCityOptions();
    }

    private void loadCategoryOptions() {
        ObservableList<String> options = FXCollections.observableArrayList();
        try {
            for (JsonNode category : apiClient.getCategoriesFlattened()) {
                int depth = category.path("depth").asInt(0);
                options.add(categoryDisplayName(category.path("name").asText(""), depth));
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
            showError("Could not load cities: " + e.getMessage());
        }
        cityComboBox.setItems(options);
    }

    @FXML
    private void handleChooseImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Images");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif"));

        List<File> files = chooser.showOpenMultipleDialog(imagePreviewPane.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            selectedImages.addAll(files);
            refreshImagePreview();
        }
    }

    private void refreshImagePreview() {
        imagePreviewPane.getChildren().clear();
        for (File file : selectedImages) {
            imagePreviewPane.getChildren().add(buildImageTile(file));
        }
    }

    private Node buildImageTile(File file) {
        ImageView imageView = new ImageView(new Image(file.toURI().toString(), 90, 70, false, true, true));
        imageView.setFitWidth(90);
        imageView.setFitHeight(70);
        imageView.setPreserveRatio(false);

        Button removeButton = new Button("\u00D7");
        removeButton.getStyleClass().add("button-danger");
        removeButton.setOnAction(event -> {
            selectedImages.remove(file);
            refreshImagePreview();
        });

        StackPane tile = new StackPane(imageView, removeButton);
        tile.getStyleClass().add("image-picker-tile");
        tile.setPrefSize(90, 70);
        StackPane.setAlignment(removeButton, Pos.TOP_RIGHT);
        return tile;
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
            Long newAdId = response.path("id").asLong();

            if (!selectedImages.isEmpty()) {
                try {
                    apiClient.uploadImages(newAdId, selectedImages);
                } catch (IOException e) {
                    // The advertisement itself was created fine; only the images failed.
                    // Still navigate through, but let the user know images didn't make it.
                    AdvertisementDetailsController.setSelectedAdvertisementId(newAdId);
                    Main.switchScene("/view/advertisement-details.fxml");
                    return;
                }
            }

            AdvertisementDetailsController.setSelectedAdvertisementId(newAdId);
            Main.switchScene("/view/advertisement-details.fxml");
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
        errorLabel.getStyleClass().removeAll("success-label");
        if (!errorLabel.getStyleClass().contains("error-label")) {
            errorLabel.getStyleClass().add("error-label");
        }
        errorLabel.setText(text);
    }
}
