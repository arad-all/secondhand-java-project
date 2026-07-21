package backend.controller.dto;

import java.time.LocalDateTime;

/** A single message within a conversation. */
public record ChatMessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderUsername,
        String content,
        LocalDateTime sentAt,
        boolean read
) {
}
