package frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.Main;
import frontend.service.ApiClient;
import frontend.service.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.IOException;

/**
 * UI logic for one conversation's message thread
 * (GET/POST /api/conversations/{id}(/messages)). Real-time chat isn't
 * required by the project spec — messages just need to be visible
 * whenever the thread is opened — so this page loads once on open and
 * again after sending or pressing Refresh, same "load on demand" style
 * used throughout this app rather than polling.
 * <p>
 * The conversation to open is passed in via a static setter, called by
 * whichever page navigated here ({@code ChatListController} or
 * {@code AdvertisementDetailsController}) — the same pattern used
 * throughout this app's navigation.
 */
public class ChatDetailController {

    private static Long conversationId;

    @FXML
    private Label titleLabel;
    @FXML
    private Label participantsLabel;
    @FXML
    private ListView<JsonNode> messagesListView;
    @FXML
    private TextField messageInputField;
    @FXML
    private Label errorLabel;

    private final ApiClient apiClient = new ApiClient();

    public static void setConversationId(Long id) {
        conversationId = id;
    }

    @FXML
    private void initialize() {
        messagesListView.setCellFactory(list -> new MessageCell());

        if (conversationId == null) {
            errorLabel.setText("No conversation selected.");
            return;
        }

        loadConversation();
    }

    private void loadConversation() {
        try {
            JsonNode conversation = apiClient.getConversation(conversationId);

            titleLabel.setText(conversation.path("advertisementTitle").asText(""));

            String myUsername = SessionManager.getInstance().getUsername();
            String buyerUsername = conversation.path("buyerUsername").asText("");
            String sellerUsername = conversation.path("sellerUsername").asText("");
            String otherParty = buyerUsername.equals(myUsername) ? sellerUsername : buyerUsername;
            participantsLabel.setText("Conversation with: " + otherParty);

            ObservableList<JsonNode> messages = FXCollections.observableArrayList();
            for (JsonNode message : conversation.path("messages")) {
                messages.add(message);
            }
            messagesListView.setItems(messages);
            if (!messages.isEmpty()) {
                messagesListView.scrollTo(messages.size() - 1);
            }

            errorLabel.setText("");
        } catch (IOException e) {
            errorLabel.setText("Could not load the conversation: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Loading the conversation was interrupted.");
        }
    }

    @FXML
    private void handleSend() {
        String content = messageInputField.getText();
        if (content == null || content.isBlank()) {
            return;
        }

        try {
            apiClient.sendMessage(conversationId, content.trim());
            messageInputField.clear();
            loadConversation();
        } catch (IOException e) {
            errorLabel.setText("Could not send message: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorLabel.setText("Sending the message was interrupted.");
        }
    }

    @FXML
    private void handleRefresh() {
        loadConversation();
    }

    @FXML
    private void handleBack() {
        try {
            Main.switchScene("/view/chat-list.fxml");
        } catch (IOException e) {
            errorLabel.setText("Could not return to the conversation list.");
        }
    }

    /**
     * Right-aligned + highlighted for the current user's own messages,
     * left-aligned + neutral for the other participant's — a plain text
     * prefix ("You: ..." vs "them: ...") would technically distinguish
     * senders too, but this reads at a glance the way a real chat app
     * does, which is what "clearly distinguish" calls for.
     */
    private static final class MessageCell extends ListCell<JsonNode> {
        @Override
        protected void updateItem(JsonNode message, boolean empty) {
            super.updateItem(message, empty);

            if (empty || message == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            String sender = message.path("senderUsername").asText("");
            String content = message.path("content").asText("");
            String sentAt = message.path("sentAt").asText("");
            String time = sentAt.length() >= 16 ? sentAt.substring(0, 16).replace('T', ' ') : sentAt;
            boolean mine = sender.equals(SessionManager.getInstance().getUsername());

            Label bubble = new Label((mine ? "You" : sender) + " · " + time + "\n" + content);
            bubble.setWrapText(true);
            bubble.setMaxWidth(360);
            bubble.setStyle(mine
                    ? "-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-padding: 8;"
                    : "-fx-background-color: #F1F0F0; -fx-background-radius: 10; -fx-padding: 8;");

            HBox row = new HBox(bubble);
            row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            setText(null);
            setGraphic(row);
        }
    }
}
