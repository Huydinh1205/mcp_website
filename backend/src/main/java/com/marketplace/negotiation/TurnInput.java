package com.marketplace.negotiation;

/**
 * price is nullable (null for accept/reject). message is nullable.
 * terms is nullable — when present it carries the full deal (quantity, freebies,
 * free shipping) and its price supersedes the bare `price` field.
 */
public record TurnInput(
    Side actor,
    TurnAction action,
    Double price,
    int roundSeen,
    String message,
    DealTerms terms) {

  /** Back-compat: price-only turn, no extra deal terms. */
  public TurnInput(Side actor, TurnAction action, Double price, int roundSeen, String message) {
    this(actor, action, price, roundSeen, message, null);
  }
}
