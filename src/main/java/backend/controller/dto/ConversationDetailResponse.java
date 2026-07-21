package backend.controller.dto;

import java.util.List;

/** Full message thread for one conversation, oldest message first. */
public record ConversationDetailResponse(
        Long id,
        Long advertisementId,
        String advertisementTitle,
        Long buyerId,
        String buyerUsername,
        Long sellerId,
        String sellerUsername,
        List<ChatMessageResponse> messages
) {
}
