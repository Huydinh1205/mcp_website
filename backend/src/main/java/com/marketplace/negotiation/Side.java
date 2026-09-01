package com.marketplace.negotiation;

public enum Side {
  BUYER,
  SELLER;

  public String wire() {
    return name().toLowerCase();
  }

  public static Side fromWire(String s) {
    return s == null ? null : Side.valueOf(s.toUpperCase());
  }
}
