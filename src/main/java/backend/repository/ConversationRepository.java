package backend.repository;

import backend.model.entity.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Conversation}. A (advertisement, buyer) pair is
 * unique at the database level, so a buyer reopening a thread with a
 * seller should look it up first via {@link #findByAdvertisementIdAndBuyerId}
 * rather than creating a duplicate.
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByAdvertisementIdAndBuyerId(Long advertisementId, Long buyerId);

    boolean existsByAdvertisementIdAndBuyerId(Long advertisementId, Long buyerId);

    List<Conversation> findByAdvertisementId(Long advertisementId);

    /**
     * All conversations a user takes part in, whether as buyer or seller.
     * An inbox list needs to show the ad title plus the other party's
     * name for every row, so advertisement/buyer/seller are fetched
     * eagerly here to avoid an N+1 per conversation.
     */
    @EntityGraph(attributePaths = {"advertisement", "buyer", "seller"})
    @Query("SELECT c FROM Conversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId")
    List<Conversation> findAllForUser(@Param("userId") Long userId);
}
