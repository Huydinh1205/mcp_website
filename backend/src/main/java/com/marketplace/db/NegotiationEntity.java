package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

/** Shared schema [Negotiation]: Negotiation_ID is bigint IDENTITY.
 *  Current_Price / Current_Freebies_Cost / Current_Free_Shipping added for this app. */
@Entity @Table(name = "Negotiation")
public class NegotiationEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Negotiation_ID") public Long id;
  @Column(name = "Buyer_ID") public Long buyerId;
  @Column(name = "Product_ID") public Long productId;
  @Column(name = "Quantity") public int quantity = 1;
  @Column(name = "Status") public String status = "open";
  @Column(name = "Date") public Instant date = Instant.now();
  @Column(name = "current_round") public int currentRound = 1;
  @Column(name = "last_actor") public String lastActor;
  @Column(name = "Final_Price") public Double finalPrice;
  @Column(name = "Order_ID") public Long orderId;
  @Column(name = "updated_at") public Instant updatedAt = Instant.now();
  @Column(name = "Current_Price") public double currentPrice;
  @Column(name = "Current_Freebies_Cost") public double currentFreebiesCost = 0;
  @Column(name = "Current_Free_Shipping") public boolean currentFreeShipping = false;
}
