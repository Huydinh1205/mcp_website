package com.marketplace.negotiation;

import java.util.List;

/**
 * A multi-term deal proposal carried by an offer/counter. This is what makes the
 * negotiation agent-worthy: the agents haggle over the whole package, not one
 * number. `freebies` are product ids the seller throws in; `freebiesCost` is
 * their resolved cost-to-seller (computed by the caller, so applyTurn stays pure
 * arithmetic).
 */
public record DealTerms(
    double price,
    int quantity,
    List<String> freebies,
    double freebiesCost,
    boolean freeShipping) {

  public static DealTerms priceOnly(double price) {
    return new DealTerms(price, 1, List.of(), 0.0, false);
  }
}
