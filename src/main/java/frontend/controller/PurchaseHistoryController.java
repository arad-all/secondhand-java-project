package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import frontend.util.AdCardFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.io.IOException;

/**
 * UI logic for the purchase-history page (GET /api/advertisements/purchased)
 * — every advertisement the logged-in caller was recorded as the buyer of
 * (i.e. via {@code AdvertisementDetailsController#handleMarkAsSold}).
 * Rating the seller happens from the advertisement details page itself
 * (see {@code AdvertisementDetailsController#handleRateSeller}) — this
 * page just lists purchases, as cards, and opens one for viewing.
 */
public class PurchaseHistoryController {

    @FXML
    private FlowPane cardGrid;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void initialize() {
        loadPurchases();
    }

    private void loadPurchases() {
        try {
            JsonNode response = apiClient.getPurchasedAdvertisements();
            cardGrid.getChildren().clear();

            for (JsonNode ad : response.path("content")) {
                cardGrid.getChildren().add(AdCardFactory.create(ad, apiClient, this::openAdvertisement));
            }

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

    private void openAdvertisement(Long id) {
        AdvertisementDetailsController.setSelectedAdvertisementId(id);
        try {
            Main.switchScene("/view/advertisement-details.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open advertisement details.");
        }
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
