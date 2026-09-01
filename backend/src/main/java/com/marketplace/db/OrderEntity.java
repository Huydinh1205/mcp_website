package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "orders")
public class OrderEntity {
  @Id @Column(name = "order_id", length = 64) public String orderId;
  @Column public String status = "pending";
  @Column(name = "order_date") public Instant orderDate = Instant.now();
  @Column(name = "buyer_confirmed_at") public Instant buyerConfirmedAt;
  @Column(name = "seller_confirmed_at") public Instant sellerConfirmedAt;
}
