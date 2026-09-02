package com.marketplace.db;

import jakarta.persistence.*;

/** Shared schema [Negotiation_Round]: composite PK (Negotiation_ID, Round_Number).
 *  Column is Sender_Type (we call it author). Terms added for this app. */
@Entity @Table(name = "Negotiation_Round")
@IdClass(NegotiationRoundId.class)
public class NegotiationRoundEntity {
  @Id @Column(name = "Negotiation_ID") public Long negotiationId;
  @Id @Column(name = "Round_Number") public int roundNumber;
  @Column(name = "Proposed_Price") public double proposedPrice;
  @Column(name = "Message_Context") public String messageContext;
  @Convert(converter = LowercaseConverter.class)
  @Column(name = "Sender_Type") public String author;
  @Column(name = "Terms") public String terms;
}
