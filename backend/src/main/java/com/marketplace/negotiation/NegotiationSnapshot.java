package com.marketplace.negotiation;

/** Everything applyTurn needs to decide a transition. lastActor / lastSellerPrice are nullable. */
public record NegotiationSnapshot(
    NegotiationStatus status,
    Side lastActor,
    int currentRound,
    double currentPrice,
    double maxBudget,
    double minPrice,
    double maxDiscountStep,
    Double lastSellerPrice) {}
