package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import frontend.service.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * UI logic for the advertisement list page. Each row in the list keeps
 * its advertisement id in a parallel list (displayedAdIds) so a
 * double-click can navigate to the matching details page without
 * needing a dedicated table-row model class.
 */
public class AdvertisementListController {

    @FXML
    private ListView<String> advertisementListView;
    @FXML
    private TextField searchField;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private final List<Long> displayedAdIds = new ArrayList<>();

    @FXML
    private void initialize() {
        loadAdvertisements();

        advertisementListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                handleViewSelected();
            }
        });
    }

    private void loadAdvertisements() {
        try {
            JsonNode advertisements = apiClient.getAdvertisements();
            ObservableList<String> rows = FXCollections.observableArrayList();
            displayedAdIds.clear();

            for (JsonNode ad : advertisements) {
                displayedAdIds.add(ad.path("id").asLong());

                String title = ad.path("title").asText("");
                String price = ad.path("price").asText("");
                String city = ad.path("cityName").asText("");
                String status = ad.path("status").asText("");

                rows.add(title + "   |   " + price + "   |   " + city + "   |   " + status);
            }

            advertisementListView.setItems(rows);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load advertisements: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading advertisements was interrupted.");
        }
    }

    @FXML
    private void handleRefresh() {
        loadAdvertisements();
    }

    @FXML
    private void handleSearch() {
        // TODO: once the backend exposes search/filter query parameters
        // (see spec section on advanced search), send searchField.getText()
        // to GET /api/advertisements?keyword=... For milestone 1 we simply
        // reload the full list.
        loadAdvertisements();
    }

    private void handleViewSelected() {
        int index = advertisementListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= displayedAdIds.size()) {
            return;
        }

        Long selectedId = displayedAdIds.get(index);
        AdvertisementDetailsController.setSelectedAdvertisementId(selectedId);

        try {
            Main.switchScene("/view/advertisement-details.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open advertisement details.");
        }
    }

    @FXML
    private void handleCreateNew() {
        try {
            Main.switchScene("/view/create-advertisement.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open the create advertisement page.");
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clear();
        try {
            Main.switchScene("/view/login.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not return to the login page.");
        }
    }
}
