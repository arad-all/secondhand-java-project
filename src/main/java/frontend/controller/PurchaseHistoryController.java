package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UI logic for the purchase-history page (GET /api/advertisements/purchased)
 * — every advertisement the logged-in caller was recorded as the buyer of
 * (i.e. via {@code AdvertisementDetailsController#handleMarkAsSold}).
 * From here the buyer can revisit the ad or rate its seller
 * (POST /api/advertisements/{id}/ratings).
 */
public class PurchaseHistoryController {

    @FXML
    private ListView<String> purchasesListView;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private final List<Long> purchasedAdIds = new ArrayList<>();

    @FXML
    private void initialize() {
        purchasesListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                handleViewSelected();
            }
        });

        loadPurchases();
    }

    private void loadPurchases() {
        try {
            JsonNode response = apiClient.getPurchasedAdvertisements();
            ObservableList<String> rows = FXCollections.observableArrayList();
            purchasedAdIds.clear();

            for (JsonNode ad : response.path("content")) {
                purchasedAdIds.add(ad.path("id").asLong());
                String title = ad.path("title").asText("");
                String price = ad.path("price").asText("");
                String city = ad.path("cityName").asText("");
                rows.add(title + "   |   " + price + "   |   " + city);
            }

            purchasesListView.setItems(rows);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load purchase history: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading purchase history was interrupted.");
        }
    }

    @FXML
    private void handleRefresh() {
        loadPurchases();
    }

    @FXML
    private void handleViewSelected() {
        Long id = selectedAdId();
        if (id == null) {
            errorLabel.setText("Select a purchased advertisement first.");
            return;
        }

        AdvertisementDetailsController.setSelectedAdvertisementId(id);
        try {
            Main.switchScene("/view/advertisement-details.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open advertisement details.");
        }
    }

    @FXML
    private void handleRateSeller() {
        Long id = selectedAdId();
        if (id == null) {
            errorLabel.setText("Select a purchased advertisement first.");
            return;
        }

        Optional<Pair<Integer, String>> result = showRatingDialog();
        if (result.isEmpty()) {
            return;
        }

        try {
            apiClient.rateSeller(id, result.get().getKey(), result.get().getValue());
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Thanks — your rating was submitted.");
        } catch (IOException e) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Could not submit rating: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    /** A small inline dialog (score 1-5 + optional comment) — no separate FXML needed for something this simple. */
    private Optional<Pair<Integer, String>> showRatingDialog() {
        Dialog<Pair<Integer, String>> dialog = new Dialog<>();
        dialog.setTitle("Rate Seller");
        dialog.setHeaderText("How was your experience with this seller?");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Integer> scoreComboBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        scoreComboBox.getSelectionModel().select(Integer.valueOf(5));
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Optional comment");
        commentArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Score (1-5):"), 0, 0);
        grid.add(scoreComboBox, 1, 0);
        grid.add(new Label("Comment:"), 0, 1);
        grid.add(commentArea, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Integer score = scoreComboBox.getValue();
                return new Pair<>(score != null ? score : 5, commentArea.getText());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private Long selectedAdId() {
        int index = purchasesListView.getSelectionModel().getSelectedIndex();
        return (index >= 0 && index < purchasedAdIds.size()) ? purchasedAdIds.get(index) : null;
    }

    @FXML
    private void handleBack() {
        try {
            Main.switchScene("/view/advertisement-list.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not return to the advertisement list.");
        }
    }
}
