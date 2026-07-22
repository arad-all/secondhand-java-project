package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * UI logic for the purchase-history page (GET /api/advertisements/purchased)
 * — every advertisement the logged-in caller was recorded as the buyer of
 * (i.e. via {@code AdvertisementDetailsController#handleMarkAsSold}).
 * Rating the seller happens from the advertisement details page itself
 * (see {@code AdvertisementDetailsController#handleRateSeller}) — this
 * page just lists purchases and opens one for viewing.
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
