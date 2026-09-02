package com.marketplace.db;

import java.io.Serializable;
import java.util.Objects;

public class NegotiationRoundId implements Serializable {
  public Long negotiationId;
  public int roundNumber;

  public NegotiationRoundId() {}

  public NegotiationRoundId(Long negotiationId, int roundNumber) {
    this.negotiationId = negotiationId;
    this.roundNumber = roundNumber;
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof NegotiationRoundId x)) return false;
    return roundNumber == x.roundNumber && Objects.equals(negotiationId, x.negotiationId);
  }

  @Override public int hashCode() {
    return Objects.hash(negotiationId, roundNumber);
  }
}
