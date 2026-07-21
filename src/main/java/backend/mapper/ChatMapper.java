package backend.mapper;

import backend.controller.dto.ChatMessageResponse;
import backend.controller.dto.ConversationDetailResponse;
import backend.controller.dto.ConversationResponse;
import backend.model.entity.ChatMessage;
import backend.model.entity.Conversation;

import java.util.List;

/** Manual entity-to-DTO mapping for chat, same convention as {@link AdvertisementMapper}. */
public final class ChatMapper {

    private ChatMapper() {
    }

    public static ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getContent(),
                message.getSentAt(),
                message.isRead());
    }

    public static ConversationResponse toConversationResponse(Conversation conversation,
                                                                ChatMessage lastMessage,
                                                                long unreadCount) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getAdvertisement().getId(),
                conversation.getAdvertisement().getTitle(),
                conversation.getBuyer().getId(),
                conversation.getBuyer().getUsername(),
                conversation.getSeller().getId(),
                conversation.getSeller().getUsername(),
                lastMessage != null ? toMessageResponse(lastMessage) : null,
                unreadCount);
    }

    public static ConversationDetailResponse toConversationDetail(Conversation conversation, List<ChatMessage> messages) {
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getAdvertisement().getId(),
                conversation.getAdvertisement().getTitle(),
                conversation.getBuyer().getId(),
                conversation.getBuyer().getUsername(),
                conversation.getSeller().getId(),
                conversation.getSeller().getUsername(),
                messages.stream().map(ChatMapper::toMessageResponse).toList());
    }
}
