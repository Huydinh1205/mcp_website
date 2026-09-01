package com.marketplace.db;

import jakarta.persistence.*;

@Entity @Table(name = "negotiation_rounds")
@IdClass(NegotiationRoundId.class)
public class NegotiationRoundEntity {
  @Id @Column(name = "negotiation_id", length = 64) public String negotiationId;
  @Id @Column(name = "round_number") public int roundNumber;
  @Column(name = "proposed_price") public double proposedPrice;
  @Column(name = "message_context") public String messageContext;
  @Column public String author;
  @Column(length = 4000) public String terms;
}
