package com.marketplace.db;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** [App_Applied_Coupon] — which coupon is attached to which negotiation. */
@Entity @Table(name = "App_Applied_Coupon")
@IdClass(AppliedDiscountEntity.Key.class)
public class AppliedDiscountEntity {
  @Id @Column(name = "Negotiation_ID") public Long negotiationId;
  @Id @Column(name = "Coupon_ID") public Long discountId;
  @Column(name = "Applied_At") public Instant appliedAt = Instant.now();

  public static class Key implements Serializable {
    public Long negotiationId;
    public Long discountId;
    public Key() {}
    public Key(Long negotiationId, Long discountId) {
      this.negotiationId = negotiationId; this.discountId = discountId;
    }
    @Override public boolean equals(Object o) {
      if (!(o instanceof Key k)) return false;
      return Objects.equals(negotiationId, k.negotiationId) && Objects.equals(discountId, k.discountId);
    }
    @Override public int hashCode() { return Objects.hash(negotiationId, discountId); }
  }
}
