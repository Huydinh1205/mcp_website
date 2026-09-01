package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "negotiations")
public class NegotiationEntity {
  @Id @Column(name = "negotiation_id", length = 64) public String negotiationId;
  @Column public String status = "open";
  @Column(name = "last_actor") public String lastActor;
  @Column(name = "current_round") public int currentRound;
  @Column(name = "current_price") public double currentPrice;
  @Column public int quantity = 1;
  @Column(name = "current_freebies_cost") public double currentFreebiesCost = 0;
  @Column(name = "current_free_shipping") public boolean currentFreeShipping = false;
  @Column public Instant date = Instant.now();
  @Column(name = "final_price") public Double finalPrice;
  @Column(name = "updated_at") public Instant updatedAt = Instant.now();
  @Column(name = "national_id", length = 64) public String nationalId;
  @Column(name = "product_id", length = 64) public String productId;
  @Column(name = "order_id", length = 64) public String orderId;
}
