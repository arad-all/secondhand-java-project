package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import frontend.service.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * UI logic for the advertisement browse/search page. Filters are sent to
 * GET /api/advertisements/search — with every filter left empty this
 * returns the same ACTIVE ads the plain GET /api/advertisements does, so
 * one code path covers both "browse everything" and "search/filter".
 * <p>
 * Each row in the list keeps its advertisement id in a parallel list
 * (displayedAdIds) so a double-click can navigate to the matching
 * details page without needing a dedicated table-row model class.
 * Category/city ComboBoxes work the same way (parallel id lists).
 */
public class AdvertisementListController {

    @FXML
    private ListView<String> advertisementListView;
    @FXML
    private TextField keywordField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private ComboBox<String> cityComboBox;
    @FXML
    private TextField minPriceField;
    @FXML
    private TextField maxPriceField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Button adminPanelButton;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private final List<Long> displayedAdIds = new ArrayList<>();

    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> cityIds = new ArrayList<>();

    private static final String[] SORT_LABELS = {
            "Newest first",
            "Price: low to high",
            "Price: high to low",
            "Seller rating: high to low"
    };
    private static final String[] SORT_FIELDS = {"createdAt", "price", "price", "sellerRating"};
    private static final String[] SORT_DIRECTIONS = {"desc", "asc", "desc", "desc"};

    @FXML
    private void initialize() {
        adminPanelButton.setVisible(SessionManager.getInstance().isAdmin());
        adminPanelButton.setManaged(SessionManager.getInstance().isAdmin());

        loadCategoryOptions();
        loadCityOptions();
        loadSortOptions();

        advertisementListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                handleViewSelected();
            }
        });

        handleSearch();
    }

    private void loadCategoryOptions() {
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("Any category");
        categoryIds.add(null);

        try {
            JsonNode categories = apiClient.getCategoriesFlattened();
            for (JsonNode category : categories) {
                int depth = category.path("depth").asInt(0);
                String name = category.path("name").asText("");
                options.add("  ".repeat(depth) + (depth > 0 ? "\u2014 " : "") + name);
                categoryIds.add(category.path("id").asLong());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            errorLabel.setText("Could not load categories: " + e.getMessage());
        }

        categoryComboBox.setItems(options);
        categoryComboBox.getSelectionModel().selectFirst();
    }

    private void loadCityOptions() {
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("Any city");
        cityIds.add(null);

        try {
            JsonNode cities = apiClient.getCities();
            for (JsonNode city : cities) {
                options.add(city.path("name").asText(""));
                cityIds.add(city.path("id").asLong());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            errorLabel.setText("Could not load cities: " + e.getMessage());
        }

        cityComboBox.setItems(options);
        cityComboBox.getSelectionModel().selectFirst();
    }

    private void loadSortOptions() {
        sortComboBox.setItems(FXCollections.observableArrayList(SORT_LABELS));
        sortComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSearch() {
        try {
            String keyword = keywordField.getText();

            Long categoryId = selectedId(categoryComboBox, categoryIds);
            Long cityId = selectedId(cityComboBox, cityIds);

            BigDecimal minPrice = parsePriceOrNull(minPriceField.getText());
            BigDecimal maxPrice = parsePriceOrNull(maxPriceField.getText());

            int sortIndex = sortComboBox.getSelectionModel().getSelectedIndex();
            String sortField = (sortIndex >= 0) ? SORT_FIELDS[sortIndex] : null;
            String sortDir = (sortIndex >= 0) ? SORT_DIRECTIONS[sortIndex] : null;

            JsonNode response = apiClient.searchAdvertisements(keyword, categoryId, cityId, minPrice, maxPrice, sortField, sortDir);
            renderResults(response.path("content"));
            errorLabel.setText("");
        } catch (NumberFormatException e) {
            errorLabel.setText("Min/max price must be valid numbers.");
        } catch (IOException e) {
            errorLabel.setText("Could not load advertisements: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading advertisements was interrupted.");
        }
    }

    private Long selectedId(ComboBox<String> comboBox, List<Long> ids) {
        int index = comboBox.getSelectionModel().getSelectedIndex();
        return (index >= 0 && index < ids.size()) ? ids.get(index) : null;
    }

    private BigDecimal parsePriceOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return new BigDecimal(text.trim());
    }

    private void renderResults(JsonNode advertisements) {
        ObservableList<String> rows = FXCollections.observableArrayList();
        displayedAdIds.clear();

        for (JsonNode ad : advertisements) {
            displayedAdIds.add(ad.path("id").asLong());

            String title = ad.path("title").asText("");
            String price = ad.path("price").asText("");
            String city = ad.path("cityName").asText("");
            String status = ad.path("status").asText("");
            String sellerRating = String.format(Locale.ROOT, "%.1f", ad.path("sellerRating").asDouble(0.0));

            rows.add(title + "   |   " + price + "   |   " + city
                    + "   |   Seller rating: " + sellerRating + "   |   " + status);
        }

        advertisementListView.setItems(rows);
    }

    @FXML
    private void handleClearFilters() {
        keywordField.clear();
        minPriceField.clear();
        maxPriceField.clear();
        categoryComboBox.getSelectionModel().selectFirst();
        cityComboBox.getSelectionModel().selectFirst();
        sortComboBox.getSelectionModel().selectFirst();
        handleSearch();
    }

    @FXML
    private void handleRefresh() {
        handleSearch();
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
    private void handleGoToMyAdvertisements() {
        try {
            Main.switchScene("/view/my-advertisements.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open your advertisements.");
        }
    }

    @FXML
    private void handleGoToFavorites() {
        try {
            Main.switchScene("/view/favorites.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open favorites.");
        }
    }

    @FXML
    private void handleGoToPurchaseHistory() {
        try {
            Main.switchScene("/view/purchase-history.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open purchase history.");
        }
    }

    @FXML
    private void handleGoToMessages() {
        try {
            Main.switchScene("/view/chat-list.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open messages.");
        }
    }

    @FXML
    private void handleGoToAdminPanel() {
        try {
            Main.switchScene("/view/admin-panel.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open the admin panel.");
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
