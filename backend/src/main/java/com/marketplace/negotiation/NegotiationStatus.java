package com.marketplace.negotiation;

public enum NegotiationStatus {
  OPEN,
  COUNTERED,
  BUYER_ACCEPTED,
  SELLER_ACCEPTED,
  CONFIRMED,
  REJECTED;

  public String wire() {
    return name().toLowerCase();
  }

  public static NegotiationStatus fromWire(String s) {
    return NegotiationStatus.valueOf(s.toUpperCase());
  }
}
