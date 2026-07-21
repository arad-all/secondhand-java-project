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
 * UI logic for the favorites page (GET/POST/DELETE /api/favorites).
 */
public class FavoritesController {

    @FXML
    private ListView<String> favoritesListView;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private final List<Long> displayedAdIds = new ArrayList<>();

    @FXML
    private void initialize() {
        favoritesListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                handleViewSelected();
            }
        });

        loadFavorites();
    }

    private void loadFavorites() {
        try {
            JsonNode favorites = apiClient.getFavorites();
            ObservableList<String> rows = FXCollections.observableArrayList();
            displayedAdIds.clear();

            for (JsonNode favorite : favorites) {
                JsonNode ad = favorite.path("advertisement");
                displayedAdIds.add(ad.path("id").asLong());

                String title = ad.path("title").asText("");
                String price = ad.path("price").asText("");
                String city = ad.path("cityName").asText("");
                String status = ad.path("status").asText("");

                rows.add(title + "   |   " + price + "   |   " + city + "   |   " + status);
            }

            favoritesListView.setItems(rows);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load favorites: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading favorites was interrupted.");
        }
    }

    @FXML
    private void handleRemoveSelected() {
        int index = favoritesListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= displayedAdIds.size()) {
            errorLabel.setText("Select a favorite to remove first.");
            return;
        }

        try {
            apiClient.removeFavorite(displayedAdIds.get(index));
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

    private void handleViewSelected() {
        int index = favoritesListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= displayedAdIds.size()) {
            return;
        }

        AdvertisementDetailsController.setSelectedAdvertisementId(displayedAdIds.get(index));
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
