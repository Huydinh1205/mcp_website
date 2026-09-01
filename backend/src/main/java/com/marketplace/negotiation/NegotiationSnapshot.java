package com.marketplace.negotiation;

/** Everything applyTurn needs to decide a transition. Nullable: lastActor, lastSellerPrice. */
public record NegotiationSnapshot(
    NegotiationStatus status,
    Side lastActor,
    int currentRound,
    double currentPrice,
    double maxBudget,
    double minPrice,
    double maxDiscountStep,
    Double lastSellerPrice,
    double shippingCost,
    double currentFreebiesCost,
    boolean currentFreeShipping) {

  /** Back-compat for tests / price-only negotiations. */
  public NegotiationSnapshot(NegotiationStatus status, Side lastActor, int currentRound,
      double currentPrice, double maxBudget, double minPrice, double maxDiscountStep,
      Double lastSellerPrice) {
    this(status, lastActor, currentRound, currentPrice, maxBudget, minPrice, maxDiscountStep,
        lastSellerPrice, 0.0, 0.0, false);
  }
}
