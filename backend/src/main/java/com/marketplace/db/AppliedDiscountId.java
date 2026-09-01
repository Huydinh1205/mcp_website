package com.marketplace.db;

import java.io.Serializable;
import java.util.Objects;

public class AppliedDiscountId implements Serializable {
  public String negotiationId;
  public String discountId;

  public AppliedDiscountId() {}
  public AppliedDiscountId(String negotiationId, String discountId) {
    this.negotiationId = negotiationId;
    this.discountId = discountId;
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof AppliedDiscountId x)) return false;
    return Objects.equals(negotiationId, x.negotiationId) && Objects.equals(discountId, x.discountId);
  }
  @Override public int hashCode() { return Objects.hash(negotiationId, discountId); }
}
