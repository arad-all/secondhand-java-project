package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
 * UI logic for editing an existing advertisement (PATCH /api/advertisements/{id}).
 * The advertisement id to edit is passed in via a static setter called by
 * the details page just before navigating here, same pattern
 * {@code AdvertisementDetailsController} uses.
 * <p>
 * Also supports managing images: viewing existing ones, removing them,
 * and adding new ones before saving.
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
    private FlowPane existingImagesPane;
    @FXML
    private Label noExistingImagesLabel;
    @FXML
    private FlowPane newImagesPreviewPane;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private final List<Long> categoryIds = new ArrayList<>();
    private final List<String> categoryPlainNames = new ArrayList<>();
    private final List<Long> cityIds = new ArrayList<>();

    /** Filenames of existing images the user has marked for removal (sent to backend on save). */
    private final List<String> removedImageFilenames = new ArrayList<>();

    /** Local files the user has chosen to add (uploaded after save if ad is editable). */
    private final List<File> newImageFiles = new ArrayList<>();

    /** Tracks the ad's current status so we know whether images can be managed. */
    private String adStatus;

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

            adStatus = ad.path("status").asText("");
            loadExistingImages(ad);
        } catch (IOException e) {
            errorLabel.setText("Could not load advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading advertisement was interrupted.");
        }
    }

    /**
     * Displays existing images from the advertisement's imageUrls array.
     * Each image gets a remove button overlaid on it. If the ad status
     * doesn't allow editing images, the remove buttons are hidden.
     */
    private void loadExistingImages(JsonNode ad) {
        existingImagesPane.getChildren().clear();
        removedImageFilenames.clear();

        boolean canManageImages = "ACTIVE".equals(adStatus)
                || "PENDING_REVIEW".equals(adStatus)
                || "REJECTED".equals(adStatus);

        int imageCount = 0;
        for (JsonNode urlNode : ad.path("imageUrls")) {
            String imageUrl = apiClient.resolveImageUrl(urlNode.asText());
            if (imageUrl == null) {
                continue;
            }

            // Extract the filename from the URL path to use for removal
            String urlPath = urlNode.asText();
            String filename = urlPath.contains("/") ? urlPath.substring(urlPath.lastIndexOf('/') + 1) : urlPath;

            ImageView imageView = new ImageView(new Image(imageUrl, 100, 75, false, true, true));
            imageView.setFitWidth(100);
            imageView.setFitHeight(75);
            imageView.setPreserveRatio(false);

            StackPane tile = new StackPane(imageView);
            tile.getStyleClass().add("image-picker-tile");
            tile.setPrefSize(100, 75);

            if (canManageImages) {
                Button removeBtn = new Button("\u00D7");
                removeBtn.getStyleClass().add("button-danger");
                removeBtn.setOnAction(event -> {
                    removedImageFilenames.add(filename);
                    existingImagesPane.getChildren().remove(tile);
                    updateNoImagesLabel();
                });
                StackPane.setAlignment(removeBtn, Pos.TOP_RIGHT);
                StackPane.setMargin(removeBtn, new Insets(2));
                tile.getChildren().add(removeBtn);
            }

            existingImagesPane.getChildren().add(tile);
            imageCount++;
        }

        updateNoImagesLabel();
    }

    private void updateNoImagesLabel() {
        boolean hasExisting = !existingImagesPane.getChildren().isEmpty();
        boolean hasNew = !newImageFiles.isEmpty();
        noExistingImagesLabel.setVisible(!hasExisting && !hasNew);
        noExistingImagesLabel.setManaged(!hasExisting && !hasNew);
    }

    // ---- New image selection ----

    @FXML
    private void handleChooseImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Images");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif"));

        List<File> files = chooser.showOpenMultipleDialog(newImagesPreviewPane.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            newImageFiles.addAll(files);
            refreshNewImagesPreview();
            updateNoImagesLabel();
        }
    }

    private void refreshNewImagesPreview() {
        newImagesPreviewPane.getChildren().clear();
        for (File file : newImageFiles) {
            newImagesPreviewPane.getChildren().add(buildNewImageTile(file));
        }
    }

    private Node buildNewImageTile(File file) {
        ImageView imageView = new ImageView(new Image(file.toURI().toString(), 90, 70, false, true, true));
        imageView.setFitWidth(90);
        imageView.setFitHeight(70);
        imageView.setPreserveRatio(false);

        Button removeButton = new Button("\u00D7");
        removeButton.getStyleClass().add("button-danger");
        removeButton.setOnAction(event -> {
            newImageFiles.remove(file);
            refreshNewImagesPreview();
            updateNoImagesLabel();
        });

        StackPane tile = new StackPane(imageView, removeButton);
        tile.getStyleClass().add("image-picker-tile");
        tile.setPrefSize(90, 70);
        StackPane.setAlignment(removeButton, Pos.TOP_RIGHT);
        return tile;
    }

    // ---- Save ----

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

            // Remove images marked for deletion
            for (String filename : removedImageFilenames) {
                try {
                    apiClient.removeImage(advertisementIdToEdit, filename);
                } catch (IOException e) {
                    // Non-fatal: log but continue — the image removal is best-effort
                    // Non-fatal: ad fields saved; image removal is best-effort
                }
            }

            // Upload new images if any were selected
            if (!newImageFiles.isEmpty()) {
                try {
                    apiClient.uploadImages(advertisementIdToEdit, newImageFiles);
                } catch (IOException e) {
                    // Non-fatal: the ad fields were saved; images just didn't make it
                    // Non-fatal: ad fields saved; image upload is best-effort
                }
            }

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

    private void showError(String text) {
        errorLabel.getStyleClass().removeAll("success-label");
        if (!errorLabel.getStyleClass().contains("error-label")) {
            errorLabel.getStyleClass().add("error-label");
        }
        errorLabel.setText(text);
    }
}
