package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import frontend.util.AdCardFactory;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.io.IOException;

/**
 * UI logic for "my advertisements" (GET /api/advertisements/my) — the
 * caller's own ads, in any status, including PENDING_REVIEW and REJECTED
 * ones a public browse would never show them. Rendered as a card grid,
 * same as the browse page (see {@link AdCardFactory}).
 */
public class MyAdvertisementsController {

    @FXML
    private FlowPane cardGrid;
    @FXML
    private ComboBox<String> statusFilterComboBox;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    private static final String[] STATUS_OPTIONS =
            {"All statuses", "PENDING_REVIEW", "ACTIVE", "REJECTED", "SOLD", "DELETED"};

    @FXML
    private void initialize() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList(STATUS_OPTIONS));
        statusFilterComboBox.getSelectionModel().selectFirst();

        loadMyAdvertisements();
    }

    private void loadMyAdvertisements() {
        try {
            int index = statusFilterComboBox.getSelectionModel().getSelectedIndex();
            String status = (index <= 0) ? null : STATUS_OPTIONS[index];

            JsonNode response = apiClient.getMyAdvertisements(status);
            cardGrid.getChildren().clear();
            for (JsonNode ad : response.path("content")) {
                cardGrid.getChildren().add(AdCardFactory.create(ad, apiClient, this::openAdvertisement));
            }

            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load your advertisements: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading was interrupted.");
        }
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
    private void handleFilterChanged() {
        loadMyAdvertisements();
    }

    @FXML
    private void handleRefresh() {
        loadMyAdvertisements();
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
