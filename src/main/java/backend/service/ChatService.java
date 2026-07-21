package backend.service;

import backend.controller.dto.ChatMessageResponse;
import backend.controller.dto.ConversationDetailResponse;
import backend.controller.dto.ConversationResponse;
import backend.controller.dto.SendMessageRequest;
import backend.exception.ForbiddenActionException;
import backend.exception.ResourceNotFoundException;
import backend.mapper.ChatMapper;
import backend.model.entity.Advertisement;
import backend.model.entity.ChatMessage;
import backend.model.entity.Conversation;
import backend.model.entity.User;
import backend.model.enums.AccountStatus;
import backend.model.enums.AdvertisementStatus;
import backend.repository.AdvertisementRepository;
import backend.repository.ChatMessageRepository;
import backend.repository.ConversationRepository;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Chat between a prospective buyer and an advertisement's seller. A
 * {@link Conversation} is scoped to exactly one (advertisement, buyer)
 * pair — the seller is always that advertisement's owner — so a buyer
 * reaching out about the same ad twice reuses the same thread instead of
 * creating a new one every time (see {@link #messageSeller}).
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AdvertisementRepository advertisementRepository;
    private final UserRepository userRepository;

    /**
     * Sends the caller's message to an advertisement's seller, creating
     * the (advertisement, buyer) conversation on first contact and
     * reusing it on every later message — so this is also how a buyer
     * sends a second, third, etc. message about the same ad, not just the
     * first one.
     * <p>
     * The advertisement must exist, not be {@code DELETED}, and not
     * belong to a {@code BLOCKED} owner — same visibility rule
     * {@link AdvertisementService#getById} enforces elsewhere, so a
     * hidden ad can't be discovered by trying to message about it. The
     * caller can't message themselves about their own ad.
     */
    @Transactional
    public ChatMessageResponse messageSeller(Long advertisementId, Long buyerId, SendMessageRequest request) {
        Advertisement ad = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> adNotFound(advertisementId));
        if (ad.getStatus() == AdvertisementStatus.DELETED || ad.getOwner().getStatus() == AccountStatus.BLOCKED) {
            throw adNotFound(advertisementId);
        }
        if (ad.getOwner().getId().equals(buyerId)) {
            throw new ForbiddenActionException("You cannot message yourself about your own advertisement.");
        }

        Conversation conversation = conversationRepository.findByAdvertisementIdAndBuyerId(advertisementId, buyerId)
                .orElseGet(() -> {
                    User buyer = userRepository.findById(buyerId)
                            .orElseThrow(() -> new ResourceNotFoundException("User with id " + buyerId + " not found."));
                    Conversation created = new Conversation();
                    created.setAdvertisement(ad);
                    created.setBuyer(buyer);
                    created.setSeller(ad.getOwner());
                    return conversationRepository.save(created);
                });

        return appendMessage(conversation, buyerId, request.content());
    }

    /**
     * Replies within an already-existing conversation. Either
     * participant — buyer or seller — may call this; anyone else gets
     * {@link ForbiddenActionException}.
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long conversationId, Long senderId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> conversationNotFound(conversationId));
        requireParticipant(conversation, senderId);

        return appendMessage(conversation, senderId, request.content());
    }

    /**
     * The caller's own conversation list ("my chats"), across every
     * advertisement, whether they're the buyer or the seller side of
     * each. Ordered newest-activity-first so an active back-and-forth
     * doesn't get buried under old, quiet threads.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(Long userId) {
        return conversationRepository.findAllForUser(userId).stream()
                .map(conversation -> toConversationResponse(conversation, userId))
                .sorted(Comparator.comparing(
                        (ConversationResponse response) -> response.lastMessage() != null
                                ? response.lastMessage().sentAt()
                                : null,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * One conversation's full message thread, oldest first. Only the
     * buyer or seller on that conversation may view it. Opening a
     * conversation this way also marks every message the caller didn't
     * send as read, same as opening a chat app's thread would.
     */
    @Transactional
    public ConversationDetailResponse getConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> conversationNotFound(conversationId));
        requireParticipant(conversation, userId);

        chatMessageRepository.markConversationAsRead(conversationId, userId);
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
        return ChatMapper.toConversationDetail(conversation, messages);
    }

    private ChatMessageResponse appendMessage(Conversation conversation, Long senderId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + senderId + " not found."));

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);

        chatMessageRepository.save(message);
        return ChatMapper.toMessageResponse(message);
    }

    private ConversationResponse toConversationResponse(Conversation conversation, Long userId) {
        ChatMessage lastMessage = chatMessageRepository
                .findTopByConversationIdOrderBySentAtDesc(conversation.getId())
                .orElse(null);
        long unreadCount = chatMessageRepository
                .countByConversationIdAndReadFalseAndSenderIdNot(conversation.getId(), userId);
        return ChatMapper.toConversationResponse(conversation, lastMessage, unreadCount);
    }

    private void requireParticipant(Conversation conversation, Long userId) {
        boolean isBuyer = conversation.getBuyer().getId().equals(userId);
        boolean isSeller = conversation.getSeller().getId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new ForbiddenActionException("You are not a participant in this conversation.");
        }
    }

    private ResourceNotFoundException adNotFound(Long advertisementId) {
        return new ResourceNotFoundException("Advertisement with id " + advertisementId + " not found.");
    }

    private ResourceNotFoundException conversationNotFound(Long conversationId) {
        return new ResourceNotFoundException("Conversation with id " + conversationId + " not found.");
    }
}
