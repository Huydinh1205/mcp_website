package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "feedback")
public class FeedbackEntity {
  @Id @Column(name = "feedback_id", length = 64) public String feedbackId;
  @Column(name = "product_id", length = 64) public String productId;
  @Column(name = "negotiation_id", length = 64) public String negotiationId;
  @Column(name = "buyer_id", length = 64) public String buyerId;
  @Column(name = "rating_score") public int ratingScore;
  @Column public String comment;
  @Column(name = "reviewer_name") public String reviewerName;
  @Column public boolean verified = true;
  @Column(name = "created_at") public Instant createdAt = Instant.now();
}
