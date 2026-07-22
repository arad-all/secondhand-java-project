package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.io.IOException;

/**
 * UI logic for the seller profile page: username/full name/phone (from
 * GET /api/users/by-username/{username}) plus average rating, rating
 * count and every review (from GET /api/users/{id}/ratings). The seller
 * to show is passed in via a static setter, called by
 * {@code AdvertisementDetailsController} just before navigating here —
 * same pattern used throughout this app's navigation.
 */
public class SellerProfileController {

    private static String sellerUsername;

    @FXML
    private Label usernameLabel;
    @FXML
    private Label fullNameLabel;
    @FXML
    private Label phoneLabel;
    @FXML
    private Label ratingSummaryLabel;
    @FXML
    private ListView<String> reviewsListView;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    public static void setSellerUsername(String username) {
        sellerUsername = username;
    }

    @FXML
    private void initialize() {
        if (sellerUsername == null || sellerUsername.isBlank()) {
            errorLabel.setText("No seller selected.");
            return;
        }

        try {
            JsonNode seller = apiClient.getUserByUsername(sellerUsername);
            Long sellerId = seller.path("id").asLong();

            usernameLabel.setText("Username: " + seller.path("username").asText(""));
            fullNameLabel.setText("Name: " + seller.path("fullName").asText(""));
            phoneLabel.setText("Phone: " + seller.path("phoneNumber").asText(""));

            JsonNode ratingsResponse = apiClient.getSellerRatings(sellerId);
            long total = ratingsResponse.path("totalRatings").asLong(0);
            double average = ratingsResponse.path("averageScore").asDouble(0.0);
            ratingSummaryLabel.setText(total == 0
                    ? "Rating: no ratings yet"
                    : String.format("Rating: %.1f / 5 (%d rating%s)", average, total, total == 1 ? "" : "s"));

            ObservableList<String> reviewRows = FXCollections.observableArrayList();
            for (JsonNode rating : ratingsResponse.path("ratings")) {
                int score = rating.path("score").asInt();
                String comment = rating.path("comment").asText("");
                String buyer = rating.path("buyerUsername").asText("");
                String adTitle = rating.path("advertisementTitle").asText("");
                String line = score + "/5 by " + buyer + " (for \"" + adTitle + "\")"
                        + (comment.isBlank() ? "" : ": " + comment);
                reviewRows.add(line);
            }
            reviewsListView.setItems(reviewRows);

            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load seller profile: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading the seller profile was interrupted.");
        }
    }

    @FXML
    private void handleBack() {
        try {
            Main.switchScene("/view/advertisement-details.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not return to the advertisement.");
        }
    }
}
