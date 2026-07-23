package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import frontend.service.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * UI logic for the conversation list ("my chats", GET /api/conversations)
 * — every conversation the caller is a buyer or seller in. Shown as
 * messenger-style rows (other participant, last message preview, time,
 * unread badge) via a custom cell — same overall page/data flow as
 * before, just a richer per-row layout than a single formatted string.
 */
public class ChatListController {

    @FXML
    private ListView<JsonNode> conversationsListView;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    @FXML
    private void initialize() {
        conversationsListView.setCellFactory(list -> new ConversationCell());

        conversationsListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                handleOpenSelected();
            }
        });

        loadConversations();
    }

    private void loadConversations() {
        try {
            JsonNode conversations = apiClient.getMyConversations();
            ObservableList<JsonNode> items = FXCollections.observableArrayList();
            for (JsonNode conversation : conversations) {
                items.add(conversation);
            }

            conversationsListView.setItems(items);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load conversations: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading conversations was interrupted.");
        }
    }

    @FXML
    private void handleRefresh() {
        loadConversations();
    }

    @FXML
    private void handleOpenSelected() {
        JsonNode selected = conversationsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            errorLabel.setText("Select a conversation first.");
            return;
        }

        ChatDetailController.setConversationId(selected.path("id").asLong());
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

    private static String formatTime(String isoDateTime) {
        return isoDateTime.length() >= 16 ? isoDateTime.substring(0, 16).replace('T', ' ') : isoDateTime;
    }

    /** One messenger-style row: other participant (prominent), ad subject, last message preview, time, unread badge. */
    private static final class ConversationCell extends ListCell<JsonNode> {
        @Override
        protected void updateItem(JsonNode conversation, boolean empty) {
            super.updateItem(conversation, empty);

            if (empty || conversation == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            String myUsername = SessionManager.getInstance().getUsername();
            String buyerUsername = conversation.path("buyerUsername").asText("");
            String sellerUsername = conversation.path("sellerUsername").asText("");
            String otherParty = buyerUsername.equals(myUsername) ? sellerUsername : buyerUsername;

            Label nameLabel = new Label(otherParty);
            nameLabel.getStyleClass().add("chat-list-name");

            Label subjectLabel = new Label(conversation.path("advertisementTitle").asText(""));
            subjectLabel.getStyleClass().add("chat-list-subject");

            JsonNode lastMessage = conversation.path("lastMessage");
            String snippet = lastMessage.path("content").asText("");
            if (snippet.length() > 45) {
                snippet = snippet.substring(0, 45) + "...";
            }
            Label previewLabel = new Label(snippet);
            previewLabel.getStyleClass().add("chat-list-preview");

            VBox textColumn = new VBox(2, nameLabel, subjectLabel, previewLabel);
            HBox.setHgrow(textColumn, Priority.ALWAYS);

            Label timeLabel = new Label(formatTime(lastMessage.path("sentAt").asText("")));
            timeLabel.getStyleClass().add("chat-list-time");

            VBox metaColumn = new VBox(8, timeLabel);
            metaColumn.setAlignment(Pos.TOP_RIGHT);

            long unreadCount = conversation.path("unreadCount").asLong(0);
            if (unreadCount > 0) {
                Label badge = new Label(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
                badge.getStyleClass().add("chat-list-badge");
                Region spacer = new Region();
                VBox.setVgrow(spacer, Priority.ALWAYS);
                metaColumn.getChildren().addAll(spacer, badge);
            }

            HBox row = new HBox(12, textColumn, metaColumn);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2));

            setText(null);
            setGraphic(row);
        }
    }
}
