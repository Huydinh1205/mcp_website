package com.marketplace.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegotiationRoundRepo
    extends JpaRepository<NegotiationRoundEntity, NegotiationRoundId> {
  List<NegotiationRoundEntity> findByNegotiationIdOrderByRoundNumberAsc(String negotiationId);
}
