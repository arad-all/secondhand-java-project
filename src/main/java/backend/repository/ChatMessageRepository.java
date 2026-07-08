package backend.repository;

import backend.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Data access for {@link ChatMessage}. Note: {@link #markConversationAsRead}
 * is a bulk update and must be called from within a transaction (e.g. a
 * {@code @Transactional} service method) for Spring Data to run it.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Rendering a conversation always needs the sender's display name for
     * every message; fetching {@code sender} eagerly here avoids one lazy
     * load per message (N+1) when the chat view is drawn.
     */
    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);

    /** Unread messages in a conversation that were not sent by the reader. */
    long countByConversationIdAndReadFalseAndSenderIdNot(Long conversationId, Long readerId);

    @Modifying
    @Query("""
            UPDATE ChatMessage m SET m.read = true
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :readerId
              AND m.read = false
            """)
    int markConversationAsRead(@Param("conversationId") Long conversationId, @Param("readerId") Long readerId);
}
