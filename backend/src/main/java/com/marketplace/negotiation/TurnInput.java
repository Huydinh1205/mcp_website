package com.marketplace.negotiation;

/** price is nullable (null for accept/reject). message is nullable. */
public record TurnInput(
    Side actor,
    TurnAction action,
    Double price,
    int roundSeen,
    String message) {}
