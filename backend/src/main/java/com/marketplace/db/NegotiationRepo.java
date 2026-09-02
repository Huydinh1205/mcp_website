package com.marketplace.db;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NegotiationRepo extends JpaRepository<NegotiationEntity, Long> {
  List<NegotiationEntity> findByBuyerId(Long buyerId);

  List<NegotiationEntity> findByProductIdInAndStatusIn(List<Long> productIds, List<String> statuses);

  List<NegotiationEntity> findByBuyerIdAndProductIdAndStatusIn(
      Long buyerId, Long productId, List<String> statuses);

  /** Optimistic write: only the row still at :expected is touched. Returns affected rows. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update NegotiationEntity n
         set n.status = :status,
             n.lastActor = :lastActor,
             n.currentRound = :nextRound,
             n.currentPrice = :nextPrice,
             n.currentFreebiesCost = :freebiesCost,
             n.currentFreeShipping = :freeShip,
             n.quantity = :qty,
             n.updatedAt = :now
       where n.id = :id
         and n.currentRound = :expected
      """)
  int applyTurn(
      @Param("id") Long id,
      @Param("now") Instant now,
      @Param("expected") int expected,
      @Param("status") String status,
      @Param("lastActor") String lastActor,
      @Param("nextRound") int nextRound,
      @Param("nextPrice") double nextPrice,
      @Param("freebiesCost") double freebiesCost,
      @Param("freeShip") boolean freeShip,
      @Param("qty") int qty);
}
