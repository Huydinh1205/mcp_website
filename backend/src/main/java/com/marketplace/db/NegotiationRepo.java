package com.marketplace.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface NegotiationRepo extends JpaRepository<NegotiationEntity, String> {
  List<NegotiationEntity> findByNationalId(String nationalId);

  List<NegotiationEntity> findByProductIdInAndStatusIn(List<String> productIds, List<String> statuses);

  List<NegotiationEntity> findByNationalIdAndProductIdAndStatusIn(
      String nationalId, String productId, List<String> statuses);

  /** Optimistic write: only the row still at :expected is touched. Returns affected rows. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update NegotiationEntity n
         set n.status = :status,
             n.lastActor = :lastActor,
             n.currentRound = :nextRound,
             n.currentPrice = :nextPrice,
             n.updatedAt = :now
       where n.negotiationId = :id
         and n.currentRound = :expected
      """)
  int applyTurn(
      @Param("id") String id,
      @Param("now") Instant now,
      @Param("expected") int expected,
      @Param("status") String status,
      @Param("lastActor") String lastActor,
      @Param("nextRound") int nextRound,
      @Param("nextPrice") double nextPrice);
}
