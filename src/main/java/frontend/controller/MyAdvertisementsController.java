package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * UI logic for "my advertisements" (GET /api/advertisements/my) — the
 * caller's own ads, in any status, including PENDING_REVIEW and REJECTED
 * ones a public browse would never show them.
 */
public class MyAdvertisementsController {

    @FXML
    private ListView<String> advertisementListView;
    @FXML
    private ComboBox<String> statusFilterComboBox;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private final List<Long> displayedAdIds = new ArrayList<>();

    private static final String[] STATUS_OPTIONS =
            {"All statuses", "PENDING_REVIEW", "ACTIVE", "REJECTED", "SOLD", "DELETED"};

    @FXML
    private void initialize() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList(STATUS_OPTIONS));
        statusFilterComboBox.getSelectionModel().selectFirst();

        advertisementListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                handleViewSelected();
            }
        });

        loadMyAdvertisements();
    }

    private void loadMyAdvertisements() {
        try {
            int index = statusFilterComboBox.getSelectionModel().getSelectedIndex();
            String status = (index <= 0) ? null : STATUS_OPTIONS[index];

            JsonNode response = apiClient.getMyAdvertisements(status);
            ObservableList<String> rows = FXCollections.observableArrayList();
            displayedAdIds.clear();

            for (JsonNode ad : response.path("content")) {
                displayedAdIds.add(ad.path("id").asLong());

                String title = ad.path("title").asText("");
                String price = ad.path("price").asText("");
                String adStatus = ad.path("status").asText("");

                rows.add(title + "   |   " + price + "   |   " + adStatus);
            }

            advertisementListView.setItems(rows);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load your advertisements: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading was interrupted.");
        }
    }

    @FXML
    private void handleFilterChanged() {
        loadMyAdvertisements();
    }

    @FXML
    private void handleRefresh() {
        loadMyAdvertisements();
    }

    private void handleViewSelected() {
        int index = advertisementListView.getSelectionModel().getSelectedIndex();
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
