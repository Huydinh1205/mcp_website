package com.marketplace.db;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * The shared schema's CHECK constraints require UPPERCASE for
 * Negotiation.last_actor and Negotiation_Round.Sender_Type ('BUYER'/'SELLER'),
 * but the rest of this app works in lowercase. Store upper, read lower.
 * NOTE: JPQL bulk updates bypass converters — NegotiationService passes an
 * already-uppercased value to the applyTurn @Query.
 */
@Converter(autoApply = false)
public class LowercaseConverter implements AttributeConverter<String, String> {
  @Override public String convertToDatabaseColumn(String v) {
    return v == null ? null : v.toUpperCase();
  }
  @Override public String convertToEntityAttribute(String v) {
    return v == null ? null : v.toLowerCase();
  }
}
