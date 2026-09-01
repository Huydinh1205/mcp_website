package com.marketplace.negotiation;

public enum TurnAction {
  OFFER,
  COUNTER,
  ACCEPT,
  REJECT;

  public static TurnAction fromWire(String s) {
    return TurnAction.valueOf(s.toUpperCase());
  }
}
