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
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * UI logic for the conversation list ("my chats", GET /api/conversations)
 * — every conversation the caller is a buyer or seller in. Per the
 * project spec's chat scenario, each row shows the advertisement's
 * title, the other participant's username, and the last message's time
 * and a snippet of its content.
 */
public class ChatListController {

    @FXML
    private ListView<String> conversationsListView;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();
    private final List<Long> conversationIds = new ArrayList<>();

    @FXML
    private void initialize() {
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
            ObservableList<String> rows = FXCollections.observableArrayList();
            conversationIds.clear();

            String myUsername = SessionManager.getInstance().getUsername();

            for (JsonNode conversation : conversations) {
                conversationIds.add(conversation.path("id").asLong());

                String buyerUsername = conversation.path("buyerUsername").asText("");
                String sellerUsername = conversation.path("sellerUsername").asText("");
                String otherParty = buyerUsername.equals(myUsername) ? sellerUsername : buyerUsername;

                String adTitle = conversation.path("advertisementTitle").asText("");

                JsonNode lastMessage = conversation.path("lastMessage");
                String snippet = lastMessage.path("content").asText("");
                if (snippet.length() > 40) {
                    snippet = snippet.substring(0, 40) + "...";
                }
                String time = formatTime(lastMessage.path("sentAt").asText(""));

                long unreadCount = conversation.path("unreadCount").asLong(0);
                String unreadTag = unreadCount > 0 ? "  (" + unreadCount + " unread)" : "";

                rows.add(adTitle + "  —  " + otherParty + "   |   " + time + "   |   " + snippet + unreadTag);
            }

            conversationsListView.setItems(rows);
            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load conversations: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading conversations was interrupted.");
        }
    }

    private String formatTime(String isoDateTime) {
        return isoDateTime.length() >= 16 ? isoDateTime.substring(0, 16).replace('T', ' ') : isoDateTime;
    }

    @FXML
    private void handleRefresh() {
        loadConversations();
    }

    @FXML
    private void handleOpenSelected() {
        int index = conversationsListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= conversationIds.size()) {
            errorLabel.setText("Select a conversation first.");
            return;
        }

        ChatDetailController.setConversationId(conversationIds.get(index));
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
}
