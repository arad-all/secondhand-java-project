package backend.controller;

import backend.controller.dto.ChatMessageResponse;
import backend.controller.dto.ConversationDetailResponse;
import backend.controller.dto.ConversationResponse;
import backend.controller.dto.SendMessageRequest;
import backend.security.AuthenticatedUser;
import backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for chat between a prospective buyer and an
 * advertisement's seller. Split across two URL prefixes, same reasoning
 * {@code RatingController} already documents for its own two prefixes:
 * <ul>
 *   <li>{@code POST /api/advertisements/{id}/messages} — "message the
 *       seller about this ad." This is the entry point: it creates the
 *       (advertisement, buyer) conversation on first contact and reuses
 *       it on every later message, so it's also how a buyer sends their
 *       second, third, etc. message about the same ad.</li>
 *   <li>{@code /api/conversations/**} — the caller's inbox (list of
 *       chats, across every ad, whether they're the buyer or the seller
 *       side) and replying within a thread once it exists. A seller's
 *       replies always go through here, since {@code /messages} above is
 *       buyer-only by design (a seller has no "advertisement id" of their
 *       own to reply through — only a conversation id).</li>
 * </ul>
 * Every route here falls under {@code SecurityConfig}'s default
 * "everything else needs a real user" rule, so {@code user} is never null
 * below.
 */
@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Sends a message to an advertisement's seller; the caller is always the buyer. */
    @PostMapping("/api/advertisements/{advertisementId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse messageSeller(
            @PathVariable Long advertisementId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return chatService.messageSeller(advertisementId, user.userId(), request);
    }

    /** The caller's own conversations — every chat they're a buyer or seller in. */
    @GetMapping("/api/conversations")
    public List<ConversationResponse> getMyConversations(@AuthenticationPrincipal AuthenticatedUser user) {
        return chatService.getMyConversations(user.userId());
    }

    /**
     * One conversation's full message thread. Only its buyer or seller
     * may view it. Also marks every message the caller didn't send as
     * read.
     */
    @GetMapping("/api/conversations/{conversationId}")
    public ConversationDetailResponse getConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return chatService.getConversation(conversationId, user.userId());
    }

    /** Replies within an existing conversation. Either participant — buyer or seller — may call this. */
    @PostMapping("/api/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return chatService.sendMessage(conversationId, user.userId(), request);
    }
}
