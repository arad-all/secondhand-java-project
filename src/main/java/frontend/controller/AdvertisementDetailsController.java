package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

/**
 * UI logic for the advertisement details page. The id of the advertisement
 * to display is passed in via a static setter called by the list page
 * just before navigating here — simple enough for this project's needs,
 * no navigation framework required.
 */
public class AdvertisementDetailsController {

    private static Long selectedAdvertisementId;

    @FXML
    private Label titleLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label cityLabel;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label ownerLabel;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    public static void setSelectedAdvertisementId(Long id) {
        selectedAdvertisementId = id;
    }

    @FXML
    private void initialize() {
        if (selectedAdvertisementId == null) {
            errorLabel.setText("No advertisement selected.");
            return;
        }

        try {
            JsonNode ad = apiClient.getAdvertisementById(selectedAdvertisementId);

            titleLabel.setText(ad.path("title").asText(""));
            descriptionLabel.setText(ad.path("description").asText(""));
            priceLabel.setText("Price: " + ad.path("price").asText(""));
            cityLabel.setText("City: " + ad.path("cityName").asText(""));
            categoryLabel.setText("Category: " + ad.path("categoryName").asText(""));
            statusLabel.setText("Status: " + ad.path("status").asText(""));
            ownerLabel.setText("Seller: " + ad.path("ownerUsername").asText(""));
        } catch (IOException e) {
            errorLabel.setText("Could not load advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading advertisement was interrupted.");
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
