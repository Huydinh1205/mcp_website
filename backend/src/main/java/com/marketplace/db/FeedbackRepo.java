package com.marketplace.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepo extends JpaRepository<FeedbackEntity, String> {
  List<FeedbackEntity> findByProductIdOrderByCreatedAtDesc(String productId);

  List<FeedbackEntity> findByNegotiationId(String negotiationId);
}
