package backend.controller.dto;

/**
 * One row of the caller's conversation list ("my chats"). Carries just
 * enough about the advertisement and the other party to render an inbox
 * row without a follow-up request; {@link #unreadCount} is scoped to the
 * caller (messages sent by the other party that the caller hasn't opened
 * yet), and {@link #lastMessage} is {@code null} only in the (currently
 * unreachable) case of a conversation with no messages at all.
 */
public record ConversationResponse(
        Long id,
        Long advertisementId,
        String advertisementTitle,
        Long buyerId,
        String buyerUsername,
        Long sellerId,
        String sellerUsername,
        ChatMessageResponse lastMessage,
        long unreadCount
) {
}
