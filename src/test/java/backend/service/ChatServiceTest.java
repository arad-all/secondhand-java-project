package backend.service;

import backend.controller.dto.ChatMessageResponse;
import backend.controller.dto.ConversationDetailResponse;
import backend.controller.dto.ConversationResponse;
import backend.controller.dto.SendMessageRequest;
import backend.exception.ForbiddenActionException;
import backend.exception.ResourceNotFoundException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private AdvertisementRepository advertisementRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private User seller;
    private User buyer;
    private User stranger;
    private Advertisement activeAd;
    private Advertisement deletedAd;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(1L);
        seller.setUsername("seller");
        seller.setStatus(AccountStatus.ACTIVE);

        buyer = new User();
        buyer.setId(2L);
        buyer.setUsername("buyer");

        stranger = new User();
        stranger.setId(3L);
        stranger.setUsername("stranger");

        activeAd = new Advertisement();
        activeAd.setId(100L);
        activeAd.setTitle("Item for Sale");
        activeAd.setStatus(AdvertisementStatus.ACTIVE);
        activeAd.setOwner(seller);

        deletedAd = new Advertisement();
        deletedAd.setId(101L);
        deletedAd.setTitle("Deleted Item");
        deletedAd.setStatus(AdvertisementStatus.DELETED);
        deletedAd.setOwner(seller);

        conversation = new Conversation();
        conversation.setId(50L);
        conversation.setAdvertisement(activeAd);
        conversation.setBuyer(buyer);
        conversation.setSeller(seller);
    }

    // ---- messageSeller ----

    @Test
    void messageSeller_createsNewConversationOnFirstContact() {
        SendMessageRequest request = new SendMessageRequest("Is this available?");
        when(advertisementRepository.findById(100L)).thenReturn(Optional.of(activeAd));
        when(conversationRepository.findByAdvertisementIdAndBuyerId(100L, 2L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(conversationRepository.save(any())).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(50L);
            return c;
        });
        when(chatMessageRepository.save(any())).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            msg.setId(200L);
            msg.setSentAt(LocalDateTime.now());
            return msg;
        });

        ChatMessageResponse result = chatService.messageSeller(100L, 2L, request);

        assertEquals("Is this available?", result.content());
        assertEquals("buyer", result.senderUsername());
    }

    @Test
    void messageSeller_reusesExistingConversation() {
        SendMessageRequest request = new SendMessageRequest("Second message");
        when(advertisementRepository.findById(100L)).thenReturn(Optional.of(activeAd));
        when(conversationRepository.findByAdvertisementIdAndBuyerId(100L, 2L))
                .thenReturn(Optional.of(conversation));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            msg.setId(201L);
            msg.setSentAt(LocalDateTime.now());
            return msg;
        });

        ChatMessageResponse result = chatService.messageSeller(100L, 2L, request);

        assertEquals("Second message", result.content());
        // Should not save a new conversation
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void messageSeller_throws_whenAdDeleted() {
        SendMessageRequest request = new SendMessageRequest("Hello?");
        when(advertisementRepository.findById(101L)).thenReturn(Optional.of(deletedAd));

        assertThrows(ResourceNotFoundException.class,
                () -> chatService.messageSeller(101L, 2L, request));
    }

    @Test
    void messageSeller_throws_whenOwnerBlocked() {
        seller.setStatus(AccountStatus.BLOCKED);
        SendMessageRequest request = new SendMessageRequest("Hello?");
        when(advertisementRepository.findById(100L)).thenReturn(Optional.of(activeAd));

        assertThrows(ResourceNotFoundException.class,
                () -> chatService.messageSeller(100L, 2L, request));
    }

    @Test
    void messageSeller_throws_whenMessagingYourself() {
        SendMessageRequest request = new SendMessageRequest("Hi me");
        when(advertisementRepository.findById(100L)).thenReturn(Optional.of(activeAd));

        assertThrows(ForbiddenActionException.class,
                () -> chatService.messageSeller(100L, 1L, request));
    }

    // ---- sendMessage ----

    @Test
    void sendMessage_buyerCanReply() {
        SendMessageRequest request = new SendMessageRequest("Sure, I'll take it");
        when(conversationRepository.findById(50L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            msg.setId(202L);
            msg.setSentAt(LocalDateTime.now());
            return msg;
        });

        ChatMessageResponse result = chatService.sendMessage(50L, 2L, request);

        assertEquals("Sure, I'll take it", result.content());
        assertEquals("buyer", result.senderUsername());
    }

    @Test
    void sendMessage_sellerCanReply() {
        SendMessageRequest request = new SendMessageRequest("Yes, still available");
        when(conversationRepository.findById(50L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            msg.setId(203L);
            msg.setSentAt(LocalDateTime.now());
            return msg;
        });

        ChatMessageResponse result = chatService.sendMessage(50L, 1L, request);

        assertEquals("Yes, still available", result.content());
        assertEquals("seller", result.senderUsername());
    }

    @Test
    void sendMessage_throws_whenNotParticipant() {
        SendMessageRequest request = new SendMessageRequest("Intercept!");
        when(conversationRepository.findById(50L)).thenReturn(Optional.of(conversation));

        assertThrows(ForbiddenActionException.class,
                () -> chatService.sendMessage(50L, 99L, request));
    }

    @Test
    void sendMessage_throws_whenConversationNotFound() {
        SendMessageRequest request = new SendMessageRequest("Hello");
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> chatService.sendMessage(999L, 2L, request));
    }

    // ---- getConversation ----

    @Test
    void getConversation_returnsMessagesAndMarksAsRead() {
        when(conversationRepository.findById(50L)).thenReturn(Optional.of(conversation));

        ChatMessage msg1 = new ChatMessage();
        msg1.setId(10L);
        msg1.setContent("First");
        msg1.setSender(buyer);
        msg1.setConversation(conversation);
        msg1.setSentAt(LocalDateTime.now().minusMinutes(5));

        ChatMessage msg2 = new ChatMessage();
        msg2.setId(11L);
        msg2.setContent("Reply");
        msg2.setSender(seller);
        msg2.setConversation(conversation);
        msg2.setSentAt(LocalDateTime.now());

        when(chatMessageRepository.findByConversationIdOrderBySentAtAsc(50L))
                .thenReturn(List.of(msg1, msg2));

        ConversationDetailResponse result = chatService.getConversation(50L, 2L);

        assertEquals(2, result.messages().size());
        assertEquals("First", result.messages().get(0).content());
        assertEquals("Reply", result.messages().get(1).content());
        // Should mark messages as read for this user
        verify(chatMessageRepository).markConversationAsRead(50L, 2L);
    }

    @Test
    void getConversation_throws_whenNotParticipant() {
        when(conversationRepository.findById(50L)).thenReturn(Optional.of(conversation));

        assertThrows(ForbiddenActionException.class,
                () -> chatService.getConversation(50L, 99L));
    }

    @Test
    void getConversation_throws_whenConversationNotFound() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> chatService.getConversation(999L, 2L));
    }

    // ---- getMyConversations ----

    @Test
    void getMyConversations_returnsSortedByLastMessageDesc() {
        // Conversation with a recent last message
        Conversation recentConv = new Conversation();
        recentConv.setId(60L);
        recentConv.setAdvertisement(activeAd);
        recentConv.setBuyer(buyer);
        recentConv.setSeller(seller);

        // Conversation with an older last message
        Conversation oldConv = new Conversation();
        oldConv.setId(61L);
        oldConv.setAdvertisement(activeAd);
        oldConv.setBuyer(buyer);
        oldConv.setSeller(seller);

        when(conversationRepository.findAllForUser(1L))
                .thenReturn(List.of(recentConv, oldConv));

        ChatMessage recentMsg = new ChatMessage();
        recentMsg.setId(20L);
        recentMsg.setContent("Recent");
        recentMsg.setSentAt(LocalDateTime.now());
        recentMsg.setSender(buyer);
        recentMsg.setConversation(recentConv);

        ChatMessage oldMsg = new ChatMessage();
        oldMsg.setId(21L);
        oldMsg.setContent("Old");
        oldMsg.setSentAt(LocalDateTime.now().minusHours(5));
        oldMsg.setSender(buyer);
        oldMsg.setConversation(oldConv);

        when(chatMessageRepository.findTopByConversationIdOrderBySentAtDesc(60L))
                .thenReturn(Optional.of(recentMsg));
        when(chatMessageRepository.findTopByConversationIdOrderBySentAtDesc(61L))
                .thenReturn(Optional.of(oldMsg));
        when(chatMessageRepository.countByConversationIdAndReadFalseAndSenderIdNot(anyLong(), eq(1L)))
                .thenReturn(0L);

        List<ConversationResponse> result = chatService.getMyConversations(1L);

        assertEquals(2, result.size());
        // Most recent should come first
        assertEquals("Recent", result.get(0).lastMessage().content());
        assertEquals("Old", result.get(1).lastMessage().content());
    }

    @Test
    void getMyConversations_handlesConversationsWithoutMessages() {
        when(conversationRepository.findAllForUser(1L))
                .thenReturn(List.of(conversation));
        when(chatMessageRepository.findTopByConversationIdOrderBySentAtDesc(50L))
                .thenReturn(Optional.empty());
        when(chatMessageRepository.countByConversationIdAndReadFalseAndSenderIdNot(50L, 1L))
                .thenReturn(0L);

        List<ConversationResponse> result = chatService.getMyConversations(1L);

        assertEquals(1, result.size());
        assertNull(result.get(0).lastMessage());
    }
}
