package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import frontend.service.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * UI logic for the advertisement details page. The id of the advertisement
 * to display is passed in via a static setter called by whichever page
 * navigated here (list, my-advertisements, favorites, purchase history,
 * admin panel) — simple enough for this project's needs, no navigation
 * framework required.
 */
public class AdvertisementDetailsController {

    /**
     * Statuses a DELETE is allowed from — mirrors
     * {@code AdvertisementService.DELETABLE_STATUSES} exactly. Editing is
     * stricter than deleting: the backend only allows PATCH while the ad
     * is ACTIVE (see {@code AdvertisementService.editAdvertisement}), so
     * REJECTED/PENDING_REVIEW ads must not offer an Edit button even
     * though they can still be deleted.
     */
    private static final Set<String> DELETABLE_STATUSES = Set.of("ACTIVE", "PENDING_REVIEW", "REJECTED");

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
    private Label buyerLabel;
    @FXML
    private Label adminNoteLabel;
    @FXML
    private Label sellerRatingLabel;
    @FXML
    private Button viewSellerProfileButton;
    @FXML
    private Button messageSellerButton;
    @FXML
    private Button rateSellerButton;
    @FXML
    private Button favoriteButton;
    @FXML
    private Button editButton;
    @FXML
    private Button markAsSoldButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button approveButton;
    @FXML
    private Button rejectButton;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private boolean isFavorite;
    private String ownerUsername;
    private String buyerUsername;
    private boolean alreadyRatedByCurrentUser;
    private Long existingConversationId;

    public static void setSelectedAdvertisementId(Long id) {
        selectedAdvertisementId = id;
    }

    @FXML
    private void initialize() {
        if (selectedAdvertisementId == null) {
            errorLabel.setText("No advertisement selected.");
            return;
        }
        loadAdvertisement();
    }

    private void loadAdvertisement() {
        try {
            JsonNode ad = apiClient.getAdvertisementById(selectedAdvertisementId);

            titleLabel.setText(ad.path("title").asText(""));
            descriptionLabel.setText(ad.path("description").asText(""));
            priceLabel.setText("Price: " + ad.path("price").asText(""));
            cityLabel.setText("City: " + ad.path("cityName").asText(""));
            categoryLabel.setText("Category: " + ad.path("categoryName").asText(""));
            String status = ad.path("status").asText("");
            statusLabel.setText("Status: " + status);
            ownerUsername = ad.path("ownerUsername").asText("");
            ownerLabel.setText("Seller: " + ownerUsername);

            String buyerUsername = ad.hasNonNull("buyerUsername") ? ad.get("buyerUsername").asText() : null;
            this.buyerUsername = buyerUsername;
            if (buyerUsername != null) {
                buyerLabel.setText("Sold to: " + buyerUsername);
                setVisible(buyerLabel, true);
            } else {
                setVisible(buyerLabel, false);
            }

            // Only meaningful once REJECTED — AdvertisementService clears
            // adminNote again on approval, so it's null otherwise.
            String adminNote = ad.hasNonNull("adminNote") ? ad.get("adminNote").asText() : null;
            if ("REJECTED".equals(status) && adminNote != null && !adminNote.isBlank()) {
                adminNoteLabel.setText("Rejection reason: " + adminNote);
                setVisible(adminNoteLabel, true);
            } else {
                setVisible(adminNoteLabel, false);
            }

            boolean loggedIn = SessionManager.getInstance().isLoggedIn();
            boolean isOwner = loggedIn && ownerUsername.equals(SessionManager.getInstance().getUsername());
            boolean isAdmin = SessionManager.getInstance().isAdmin();

            configureFavoriteButton(loggedIn);
            configureOwnerButtons(isOwner, status);
            configureAdminButtons(isAdmin, status);
            configureMessagingButton(loggedIn, isOwner);
            loadSellerRatingSummary(ownerUsername);
            configureRateSellerButton(loggedIn, status);

            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading advertisement was interrupted.");
        }
    }

    /**
     * Per the project spec's "view advertisement details" scenario, the
     * seller's average rating is shown right on this page (the full
     * review list lives on the dedicated seller-profile page instead —
     * see {@link #handleViewSellerProfile}). Resolves the seller's
     * numeric id from their username first, since that's all an
     * {@code AdvertisementDetailResponse} carries.
     * <p>
     * Also determines {@link #alreadyRatedByCurrentUser} from the same
     * response — {@code RatingResponse} already carries the
     * advertisementId and buyerUsername of every rating, so no extra
     * call is needed to check whether the buyer already rated this
     * specific purchase (used by {@link #configureRateSellerButton}).
     */
    private void loadSellerRatingSummary(String ownerUsername) {
        alreadyRatedByCurrentUser = false;
        try {
            JsonNode seller = apiClient.getUserByUsername(ownerUsername);
            Long sellerId = seller.path("id").asLong();

            JsonNode ratings = apiClient.getSellerRatings(sellerId);
            long total = ratings.path("totalRatings").asLong(0);
            double average = ratings.path("averageScore").asDouble(0.0);

            sellerRatingLabel.setText(total == 0
                    ? "Seller rating: no ratings yet"
                    : String.format("Seller rating: %.1f / 5 (%d rating%s)", average, total, total == 1 ? "" : "s"));
            setVisible(sellerRatingLabel, true);
            setVisible(viewSellerProfileButton, true);

            String myUsername = SessionManager.getInstance().getUsername();
            if (myUsername != null) {
                for (JsonNode rating : ratings.path("ratings")) {
                    if (rating.path("advertisementId").asLong() == selectedAdvertisementId
                            && myUsername.equals(rating.path("buyerUsername").asText())) {
                        alreadyRatedByCurrentUser = true;
                        break;
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // Non-fatal: just hide these rather than blocking the whole page.
            setVisible(sellerRatingLabel, false);
            setVisible(viewSellerProfileButton, false);
        }
    }

    /**
     * The buyer of a SOLD ad may rate its seller exactly once — visible
     * only for that buyer, only once SOLD, and only if
     * {@link #loadSellerRatingSummary} didn't already find an existing
     * rating from them for this ad. Moved here from the purchase-history
     * page so there's a single place to rate a seller.
     */
    private void configureRateSellerButton(boolean loggedIn, String status) {
        boolean isBuyer = loggedIn && buyerUsername != null
                && buyerUsername.equals(SessionManager.getInstance().getUsername());
        setVisible(rateSellerButton, isBuyer && "SOLD".equals(status) && !alreadyRatedByCurrentUser);
    }

    private void configureFavoriteButton(boolean loggedIn) {
        if (!loggedIn) {
            setVisible(favoriteButton, false);
            return;
        }

        try {
            isFavorite = false;
            for (JsonNode favorite : apiClient.getFavorites()) {
                if (favorite.path("advertisement").path("id").asLong() == selectedAdvertisementId) {
                    isFavorite = true;
                    break;
                }
            }
            favoriteButton.setText(isFavorite ? "Remove from Favorites" : "Add to Favorites");
            setVisible(favoriteButton, true);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // Non-fatal: just hide the button rather than blocking the whole page.
            setVisible(favoriteButton, false);
        }
    }

    private void configureOwnerButtons(boolean isOwner, String status) {
        // Editing is only ever allowed while ACTIVE (see AdvertisementService.editAdvertisement) —
        // in particular, a REJECTED ad can no longer be edited, only deleted or resubmitted as new.
        setVisible(editButton, isOwner && "ACTIVE".equals(status));
        setVisible(deleteButton, isOwner && DELETABLE_STATUSES.contains(status));
        setVisible(markAsSoldButton, isOwner && "ACTIVE".equals(status));
    }

    private void configureAdminButtons(boolean isAdmin, String status) {
        boolean pending = isAdmin && "PENDING_REVIEW".equals(status);
        setVisible(approveButton, pending);
        setVisible(rejectButton, pending);
        // An admin may also delete an ad directly from here, on top of the
        // owner's own delete button configured above.
        if (isAdmin && DELETABLE_STATUSES.contains(status)) {
            setVisible(deleteButton, true);
        }
    }

    /**
     * Per the spec's "start a conversation with the seller" scenario,
     * this lives right here on the ad details page. Looks through the
     * caller's own conversations (GET /api/conversations) for one already
     * about this ad — if found, the button just opens it (no duplicate
     * conversation is ever created, since ChatService#messageSeller reuses
     * the existing one anyway; checking here up front just avoids
     * re-prompting for a message when there's already a thread to open).
     * Hidden entirely for the ad's own owner, same restriction
     * ChatService#messageSeller enforces server-side.
     */
    private void configureMessagingButton(boolean loggedIn, boolean isOwner) {
        existingConversationId = null;
        if (!loggedIn || isOwner) {
            setVisible(messageSellerButton, false);
            return;
        }

        try {
            for (JsonNode conversation : apiClient.getMyConversations()) {
                if (conversation.path("advertisementId").asLong() == selectedAdvertisementId) {
                    existingConversationId = conversation.path("id").asLong();
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // Non-fatal: worst case the button just offers to start a new
            // message instead of jumping straight to an existing thread.
        }

        messageSellerButton.setText(existingConversationId != null ? "View Conversation" : "Message Seller");
        setVisible(messageSellerButton, true);
    }

    private void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    @FXML
    private void handleToggleFavorite() {
        try {
            if (isFavorite) {
                apiClient.removeFavorite(selectedAdvertisementId);
            } else {
                apiClient.addFavorite(selectedAdvertisementId);
            }
            isFavorite = !isFavorite;
            favoriteButton.setText(isFavorite ? "Remove from Favorites" : "Add to Favorites");
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not update favorites: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Updating favorites was interrupted.");
        }
    }

    @FXML
    private void handleEdit() {
        EditAdvertisementController.setAdvertisementIdToEdit(selectedAdvertisementId);
        try {
            Main.switchScene("/view/edit-advertisement.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open the edit page.");
        }
    }

    @FXML
    private void handleMarkAsSold() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Mark as Sold");
        dialog.setHeaderText("Enter the buyer's username.");
        dialog.setContentText("Buyer username:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            return;
        }

        String buyerUsername = result.get().trim();
        try {
            JsonNode buyer = apiClient.getUserByUsername(buyerUsername);
            Long buyerId = buyer.path("id").asLong();
            apiClient.markAsSold(selectedAdvertisementId, buyerId);
            loadAdvertisement();
        } catch (IOException e) {
            errorLabel.setText("Could not mark as sold: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this advertisement? This cannot be undone.", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        try {
            apiClient.deleteAdvertisement(selectedAdvertisementId);
            handleBack();
        } catch (IOException e) {
            errorLabel.setText("Could not delete advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    @FXML
    private void handleApprove() {
        try {
            apiClient.approveAdvertisement(selectedAdvertisementId);
            loadAdvertisement();
        } catch (IOException e) {
            errorLabel.setText("Could not approve advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    @FXML
    private void handleReject() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Advertisement");
        dialog.setHeaderText("Why is this advertisement being rejected?");
        dialog.setContentText("Reason:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            return;
        }

        try {
            apiClient.rejectAdvertisement(selectedAdvertisementId, result.get().trim());
            loadAdvertisement();
        } catch (IOException e) {
            errorLabel.setText("Could not reject advertisement: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    @FXML
    private void handleViewSellerProfile() {
        if (ownerUsername == null || ownerUsername.isBlank()) {
            return;
        }
        SellerProfileController.setSellerUsername(ownerUsername);
        try {
            Main.switchScene("/view/seller-profile.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open the seller's profile.");
        }
    }

    /**
     * Opens the chat page immediately — no popup first. If
     * {@link #configureMessagingButton} already found an existing
     * conversation about this ad, it opens that; otherwise it opens the
     * chat page in its "pending" state (see {@code ChatDetailController}),
     * where the buyer types their first message inside the normal chat
     * view, same as replying to any other conversation.
     */
    @FXML
    private void handleMessageSeller() {
        if (existingConversationId != null) {
            ChatDetailController.setConversationId(existingConversationId);
        } else {
            ChatDetailController.setPendingConversation(selectedAdvertisementId, titleLabel.getText(), ownerUsername);
        }

        try {
            Main.switchScene("/view/chat-detail.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not open the conversation.");
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

    /**
     * Moved here from the purchase-history page so there's a single
     * place to rate a seller (POST /api/advertisements/{id}/ratings).
     * The backend ({@code RatingService#rate}) is still the source of
     * truth for "already rated" — this button is just hidden once
     * {@link #loadSellerRatingSummary} already found an existing rating,
     * so in practice this duplicate-submission path is rarely hit, but
     * it's kept as a safety net (e.g. a rating submitted from another
     * session since this page loaded).
     */
    @FXML
    private void handleRateSeller() {
        Optional<Pair<Integer, String>> result = showRatingDialog();
        if (result.isEmpty()) {
            return;
        }

        try {
            apiClient.rateSeller(selectedAdvertisementId, result.get().getKey(), result.get().getValue());
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Thanks — your rating was submitted.");
            loadAdvertisement();
        } catch (IOException e) {
            String message = e.getMessage();
            errorLabel.setStyle("-fx-text-fill: red;");
            if (message != null && message.toLowerCase().contains("already rated")) {
                errorLabel.setText("You have already submitted a rating for this purchase.");
                setVisible(rateSellerButton, false);
            } else {
                errorLabel.setText("Could not submit rating: " + message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("The request was interrupted.");
        }
    }

    /** A small inline dialog (score 1-5 + optional comment) — no separate FXML needed for something this simple. */
    private Optional<Pair<Integer, String>> showRatingDialog() {
        Dialog<Pair<Integer, String>> dialog = new Dialog<>();
        dialog.setTitle("Rate Seller");
        dialog.setHeaderText("How was your experience with this seller?");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Integer> scoreComboBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        scoreComboBox.getSelectionModel().select(Integer.valueOf(5));
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Optional comment");
        commentArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Score (1-5):"), 0, 0);
        grid.add(scoreComboBox, 1, 0);
        grid.add(new Label("Comment:"), 0, 1);
        grid.add(commentArea, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Integer score = scoreComboBox.getValue();
                return new Pair<>(score != null ? score : 5, commentArea.getText());
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
