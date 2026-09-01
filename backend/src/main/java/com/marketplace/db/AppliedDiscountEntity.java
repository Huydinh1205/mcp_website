package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "applied_discounts")
@IdClass(AppliedDiscountId.class)
public class AppliedDiscountEntity {
  @Id @Column(name = "negotiation_id", length = 64) public String negotiationId;
  @Id @Column(name = "discount_id", length = 64) public String discountId;
  @Column(name = "applied_at") public Instant appliedAt = Instant.now();
}
