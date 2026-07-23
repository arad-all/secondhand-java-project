package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import frontend.util.AdCardFactory;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * UI logic for the favorites page (GET/POST/DELETE /api/favorites).
 * Each favorite is a card (see {@link AdCardFactory}) with a small
 * "Remove" button underneath, since unfavoriting is specific to this
 * page and doesn't belong on the shared card itself.
 */
public class FavoritesController {

    @FXML
    private FlowPane cardGrid;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void initialize() {
        loadFavorites();
    }

    private void loadFavorites() {
        try {
            JsonNode favorites = apiClient.getFavorites();
            cardGrid.getChildren().clear();

            for (JsonNode favorite : favorites) {
                JsonNode ad = favorite.path("advertisement");
                long adId = ad.path("id").asLong();

                VBox card = AdCardFactory.create(ad, apiClient, this::openAdvertisement);

                Button removeButton = new Button("Remove from Favorites");
                removeButton.getStyleClass().add("button-danger");
                removeButton.setMaxWidth(Double.MAX_VALUE);
                removeButton.setOnAction(event -> removeFavorite(adId));

                VBox wrapper = new VBox(6, card, removeButton);
                wrapper.setAlignment(Pos.TOP_CENTER);
                cardGrid.getChildren().add(wrapper);
            }

            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load favorites: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading favorites was interrupted.");
        }
    }

    private void removeFavorite(long advertisementId) {
        try {
            apiClient.removeFavorite(advertisementId);
            loadFavorites();
        } catch (IOException e) {
            errorLabel.setText("Could not remove favorite: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    @FXML
    private void handleRefresh() {
        loadFavorites();
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
