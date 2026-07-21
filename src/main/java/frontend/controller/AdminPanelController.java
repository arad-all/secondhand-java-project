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
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UI logic for the admin panel: advertisement moderation (pending queue,
 * approve/reject), user management (list/filter, block/unblock), and
 * category/city reference-data management (view + create). All four
 * tabs share this one controller since they're small and always used
 * together by an admin — no need for separate pages.
 */
public class AdminPanelController {

    @FXML
    private ListView<String> pendingListView;
    @FXML
    private ListView<String> usersListView;
    @FXML
    private ComboBox<String> userStatusFilterComboBox;
    @FXML
    private ListView<String> categoriesListView;
    @FXML
    private TextField newCategoryNameField;
    @FXML
    private ComboBox<String> newCategoryParentComboBox;
    @FXML
    private ListView<String> citiesListView;
    @FXML
    private TextField newCityNameField;
    @FXML
    private TextField newCityProvinceField;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    private final List<Long> pendingAdIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> categoryParentIds = new ArrayList<>();

    private static final String[] USER_STATUS_OPTIONS = {"All statuses", "ACTIVE", "BLOCKED"};

    @FXML
    private void initialize() {
        userStatusFilterComboBox.setItems(FXCollections.observableArrayList(USER_STATUS_OPTIONS));
        userStatusFilterComboBox.getSelectionModel().selectFirst();

        loadPendingAdvertisements();
        loadUsers();
        loadCategories();
        loadCities();
    }

    // ------------------------------------------------------------------
    // Pending advertisements tab
    // ------------------------------------------------------------------

    private void loadPendingAdvertisements() {
        try {
            JsonNode response = apiClient.listPendingAdvertisements();
            ObservableList<String> rows = FXCollections.observableArrayList();
            pendingAdIds.clear();

            for (JsonNode ad : response.path("content")) {
                pendingAdIds.add(ad.path("id").asLong());
                String title = ad.path("title").asText("");
                String price = ad.path("price").asText("");
                String city = ad.path("cityName").asText("");
                rows.add(title + "   |   " + price + "   |   " + city);
            }

            pendingListView.setItems(rows);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load pending advertisements: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading was interrupted.");
        }
    }

    @FXML
    private void handleRefreshPending() {
        loadPendingAdvertisements();
    }

    @FXML
    private void handleViewPendingSelected() {
        Long id = selectedPendingId();
        if (id == null) {
            return;
        }
        AdvertisementDetailsController.setSelectedAdvertisementId(id);
        try {
            Main.switchScene("/view/advertisement-details.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open advertisement details.");
        }
    }

    @FXML
    private void handleApproveSelected() {
        Long id = selectedPendingId();
        if (id == null) {
            errorLabel.setText("Select a pending advertisement first.");
            return;
        }

        try {
            apiClient.approveAdvertisement(id);
            loadPendingAdvertisements();
        } catch (IOException e) {
            errorLabel.setText("Could not approve advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    @FXML
    private void handleRejectSelected() {
        Long id = selectedPendingId();
        if (id == null) {
            errorLabel.setText("Select a pending advertisement first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Advertisement");
        dialog.setHeaderText("Why is this advertisement being rejected?");
        dialog.setContentText("Reason:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            return;
        }

        try {
            apiClient.rejectAdvertisement(id, result.get().trim());
            loadPendingAdvertisements();
        } catch (IOException e) {
            errorLabel.setText("Could not reject advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    private Long selectedPendingId() {
        int index = pendingListView.getSelectionModel().getSelectedIndex();
        return (index >= 0 && index < pendingAdIds.size()) ? pendingAdIds.get(index) : null;
    }

    // ------------------------------------------------------------------
    // Users tab
    // ------------------------------------------------------------------

    private void loadUsers() {
        try {
            int index = userStatusFilterComboBox.getSelectionModel().getSelectedIndex();
            String status = (index <= 0) ? null : USER_STATUS_OPTIONS[index];

            JsonNode users = apiClient.listUsers(status);
            ObservableList<String> rows = FXCollections.observableArrayList();
            userIds.clear();

            for (JsonNode user : users) {
                userIds.add(user.path("id").asLong());
                String username = user.path("username").asText("");
                String fullName = user.path("fullName").asText("");
                String role = user.path("role").asText("");
                String status2 = user.path("status").asText("");
                rows.add(username + "   |   " + fullName + "   |   " + role + "   |   " + status2);
            }

            usersListView.setItems(rows);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load users: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading users was interrupted.");
        }
    }

    @FXML
    private void handleRefreshUsers() {
        loadUsers();
    }

    @FXML
    private void handleBlockSelectedUser() {
        Long id = selectedUserId();
        if (id == null) {
            errorLabel.setText("Select a user first.");
            return;
        }

        try {
            apiClient.blockUser(id);
            loadUsers();
        } catch (IOException e) {
            errorLabel.setText("Could not block user: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    @FXML
    private void handleUnblockSelectedUser() {
        Long id = selectedUserId();
        if (id == null) {
            errorLabel.setText("Select a user first.");
            return;
        }

        try {
            apiClient.unblockUser(id);
            loadUsers();
        } catch (IOException e) {
            errorLabel.setText("Could not unblock user: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    private Long selectedUserId() {
        int index = usersListView.getSelectionModel().getSelectedIndex();
        return (index >= 0 && index < userIds.size()) ? userIds.get(index) : null;
    }

    // ------------------------------------------------------------------
    // Categories tab
    // ------------------------------------------------------------------

    /**
     * Loads every category (top-level entries, each immediately followed
     * by its own children) via {@link ApiClient#getCategoriesFlattened()},
     * and also refreshes the "parent" ComboBox used when creating a new
     * subcategory.
     */
    private void loadCategories() {
        try {
            JsonNode categories = apiClient.getCategoriesFlattened();
            ObservableList<String> rows = FXCollections.observableArrayList();

            ObservableList<String> parentOptions = FXCollections.observableArrayList();
            parentOptions.add("None (top-level category)");
            categoryParentIds.clear();
            categoryParentIds.add(null);

            for (JsonNode category : categories) {
                boolean isSubcategory = category.hasNonNull("parentId");
                String name = category.path("name").asText("");
                String displayName = (isSubcategory ? "\u2014 " : "") + name;

                rows.add(displayName + "   (id " + category.path("id").asText("") + ")");

                parentOptions.add(displayName);
                categoryParentIds.add(category.path("id").asLong());
            }

            categoriesListView.setItems(rows);

            newCategoryParentComboBox.setItems(parentOptions);
            newCategoryParentComboBox.getSelectionModel().selectFirst();

            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load categories: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading categories was interrupted.");
        }
    }

    @FXML
    private void handleRefreshCategories() {
        loadCategories();
    }

    @FXML
    private void handleCreateCategory() {
        String name = newCategoryNameField.getText();
        if (name == null || name.isBlank()) {
            errorLabel.setText("Category name is required.");
            return;
        }

        int parentIndex = newCategoryParentComboBox.getSelectionModel().getSelectedIndex();
        Long parentId = (parentIndex >= 0 && parentIndex < categoryParentIds.size())
                ? categoryParentIds.get(parentIndex) : null;

        try {
            apiClient.createCategory(name.trim(), parentId);
            newCategoryNameField.clear();
            loadCategories();
        } catch (IOException e) {
            errorLabel.setText("Could not create category: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    // Note: there is no "delete category" handler here — the backend
    // (CategoryController/CategoryService) does not expose a delete
    // operation yet. See the final report for details.

    // ------------------------------------------------------------------
    // Cities tab
    // ------------------------------------------------------------------

    private void loadCities() {
        try {
            JsonNode cities = apiClient.getCities();
            ObservableList<String> rows = FXCollections.observableArrayList();

            for (JsonNode city : cities) {
                String name = city.path("name").asText("");
                String province = city.path("province").asText("");
                String label = province.isBlank() ? name : (name + ", " + province);
                rows.add(label + "   (id " + city.path("id").asText("") + ")");
            }

            citiesListView.setItems(rows);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load cities: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading cities was interrupted.");
        }
    }

    @FXML
    private void handleRefreshCities() {
        loadCities();
    }

    @FXML
    private void handleCreateCity() {
        String name = newCityNameField.getText();
        if (name == null || name.isBlank()) {
            errorLabel.setText("City name is required.");
            return;
        }

        try {
            apiClient.createCity(name.trim(), newCityProvinceField.getText());
            newCityNameField.clear();
            newCityProvinceField.clear();
            loadCities();
        } catch (IOException e) {
            errorLabel.setText("Could not create city: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    // Note: there is no "delete city" handler here — the backend
    // (CityController/CityService) does not expose a delete operation
    // yet. See the final report for details.

    @FXML
    private void handleBack() {
        try {
            Main.switchScene("/view/advertisement-list.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not return to the advertisement list.");
        }
    }
}
