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

  /** A new transcript row to persist. Null for accept / reject / round-cap.
   *  `terms` carries the full deal when the round had multi-term content. */
  public record AppendRound(Side author, double proposedPrice, String message, DealTerms terms) {
    public AppendRound(Side author, double proposedPrice, String message) {
      this(author, proposedPrice, message, null);
    }
  }
}
