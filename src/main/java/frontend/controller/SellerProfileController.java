package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
    private Label ratingStarLabel;
    @FXML
    private Label ratingSummaryLabel;
    @FXML
    private ListView<JsonNode> reviewsListView;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    public static void setSellerUsername(String username) {
        sellerUsername = username;
    }

    @FXML
    private void initialize() {
        reviewsListView.setCellFactory(list -> new ReviewCell());
        reviewsListView.setPlaceholder(new Label("No reviews yet."));

        if (sellerUsername == null || sellerUsername.isBlank()) {
            errorLabel.setText("No seller selected.");
            return;
        }

        try {
            JsonNode seller = apiClient.getUserByUsername(sellerUsername);
            Long sellerId = seller.path("id").asLong();

            usernameLabel.setText(seller.path("username").asText(""));
            fullNameLabel.setText(seller.path("fullName").asText(""));
            phoneLabel.setText(seller.path("phoneNumber").asText(""));

            JsonNode ratingsResponse = apiClient.getSellerRatings(sellerId);
            long total = ratingsResponse.path("totalRatings").asLong(0);
            double average = ratingsResponse.path("averageScore").asDouble(0.0);

            // Same star + "avg (count)" format as the seller-rating row on the
            // advertisement details page, for a consistent look across the app.
            if (total == 0) {
                ratingStarLabel.setVisible(false);
                ratingStarLabel.setManaged(false);
                ratingSummaryLabel.setText("No ratings yet");
            } else {
                ratingStarLabel.setText("★");
                ratingStarLabel.setVisible(true);
                ratingStarLabel.setManaged(true);
                ratingSummaryLabel.setText(String.format("%.1f (%d rating%s)", average, total, total == 1 ? "" : "s"));
            }

            ObservableList<JsonNode> reviewItems = FXCollections.observableArrayList();
            for (JsonNode rating : ratingsResponse.path("ratings")) {
                reviewItems.add(rating);
            }
            reviewsListView.setItems(reviewItems);

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

    /**
     * One review as a small card: a 5-star row (filled stars in the
     * accent color, empty stars muted) with the buyer's name alongside,
     * the advertisement it was left for underneath, and the comment (if
     * any) below that — replaces the old single "score/5 by buyer ..."
     * text line with something that's actually scannable.
     */
    private static final class ReviewCell extends ListCell<JsonNode> {
        @Override
        protected void updateItem(JsonNode rating, boolean empty) {
            super.updateItem(rating, empty);

            if (empty || rating == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            int score = Math.max(0, Math.min(5, rating.path("score").asInt()));
            String buyer = rating.path("buyerUsername").asText("");
            String adTitle = rating.path("advertisementTitle").asText("");
            String comment = rating.path("comment").asText("");

            Label filledStars = new Label("★".repeat(score));
            filledStars.getStyleClass().add("ad-card-rating-star");
            Label emptyStars = new Label("★".repeat(5 - score));
            emptyStars.getStyleClass().add("review-star-empty");

            Label buyerLabel = new Label(buyer);
            buyerLabel.getStyleClass().add("review-buyer-label");

            HBox starsBox = new HBox(filledStars, emptyStars);
            starsBox.setAlignment(Pos.CENTER_LEFT);

            HBox headerRow = new HBox(8, starsBox, buyerLabel);
            headerRow.setAlignment(Pos.CENTER_LEFT);

            Label adTitleLabel = new Label("For \"" + adTitle + "\"");
            adTitleLabel.getStyleClass().add("subtle-label");

            VBox content = new VBox(4, headerRow, adTitleLabel);

            if (!comment.isBlank()) {
                Label commentLabel = new Label(comment);
                commentLabel.setWrapText(true);
                commentLabel.getStyleClass().add("review-comment-label");
                content.getChildren().add(commentLabel);
            }

            setText(null);
            setGraphic(content);
        }
    }
}