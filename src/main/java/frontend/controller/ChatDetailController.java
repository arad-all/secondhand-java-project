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
import javafx.scene.layout.VBox;

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
 * <p>
 * There's also a "pending" entry point ({@link #setPendingConversation}):
 * when there's no conversation yet for an ad, this page still opens
 * immediately (per the spec, a buyer should type their first message
 * inside the normal chat view, not a popup beforehand) showing just the
 * ad title/seller and an empty thread. The first {@link #handleSend}
 * then calls {@code POST /api/advertisements/{id}/messages} instead of
 * the usual reply endpoint — that's the same call that creates the
 * conversation on the backend — and once it returns, this page has a
 * real conversationId and behaves exactly like the normal case from
 * then on.
 */
public class ChatDetailController {

    private static Long conversationId;
    private static Long pendingAdvertisementId;
    private static String pendingAdvertisementTitle;
    private static String pendingSellerUsername;

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

    /** Opens an existing conversation directly. */
    public static void setConversationId(Long id) {
        conversationId = id;
        pendingAdvertisementId = null;
    }

    /** Opens the chat page for an ad with no conversation yet — see the class Javadoc. */
    public static void setPendingConversation(Long advertisementId, String advertisementTitle, String sellerUsername) {
        conversationId = null;
        pendingAdvertisementId = advertisementId;
        pendingAdvertisementTitle = advertisementTitle;
        pendingSellerUsername = sellerUsername;
    }

    @FXML
    private void initialize() {
        messagesListView.setCellFactory(list -> new MessageCell());
        messagesListView.setPlaceholder(new Label("No messages yet — say hello!"));

        if (conversationId != null) {
            loadConversation();
        } else if (pendingAdvertisementId != null) {
            titleLabel.setText(pendingAdvertisementTitle);
            participantsLabel.setText("Conversation with: " + pendingSellerUsername);
            messagesListView.setItems(FXCollections.observableArrayList());
        } else {
            errorLabel.setText("No conversation selected.");
        }
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
            if (conversationId != null) {
                apiClient.sendMessage(conversationId, content.trim());
            } else {
                // First message for this ad — this single call both creates
                // the conversation on the backend and sends the message.
                JsonNode message = apiClient.messageSeller(pendingAdvertisementId, content.trim());
                conversationId = message.path("conversationId").asLong();
                pendingAdvertisementId = null;
            }
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
        if (conversationId != null) {
            loadConversation();
        }
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
     * Right-aligned + blue for the current user's own messages,
     * left-aligned + neutral for the other participant's — reads at a
     * glance the way a real chat app does. Sender name sits above the
     * message body, and the timestamp is tucked in the bottom-right of
     * the bubble, both styled via app.css (see the CHAT STYLES section)
     * rather than inline — keeps this class free of style strings.
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

            Label senderLabel = new Label(mine ? "You" : sender);
            senderLabel.getStyleClass().add("chat-bubble-sender");

            Label contentLabel = new Label(content);
            contentLabel.setWrapText(true);
            contentLabel.setMaxWidth(340);

            Label timeLabel = new Label(time);
            timeLabel.getStyleClass().add("chat-bubble-time");
            HBox timeRow = new HBox(timeLabel);
            timeRow.setAlignment(Pos.CENTER_RIGHT);

            VBox bubble = new VBox(3, senderLabel, contentLabel, timeRow);
            bubble.getStyleClass().add(mine ? "chat-bubble-mine" : "chat-bubble-other");
            bubble.setMaxWidth(380);

            HBox row = new HBox(bubble);
            row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            setText(null);
            setGraphic(row);
        }
    }
}
