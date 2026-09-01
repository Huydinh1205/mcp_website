package com.marketplace.negotiation;

public record TurnOutcome(
    NegotiationStatus nextStatus,
    Side nextLastActor,
    int nextRound,
    double nextPrice,
    boolean terminal,
    boolean requiresHumanConfirmation,
    AppendRound appendRound,
    String reason) {

  /** A new transcript row to persist. Null for accept / reject / round-cap. */
  public record AppendRound(Side author, double proposedPrice, String message) {}
}
