package com.marketplace.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepo extends JpaRepository<FeedbackEntity, FeedbackEntity.Key> {
  List<FeedbackEntity> findByProductIdOrderByCreatedAtDesc(Long productId);
  List<FeedbackEntity> findByInvoiceId(Long invoiceId);
}
