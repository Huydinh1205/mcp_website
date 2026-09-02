package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

/** Shared schema [Order]: Order_ID is bigint IDENTITY.
 *  Buyer_Confirmed_At / Seller_Confirmed_At added for this app's two-sided confirm. */
@Entity @Table(name = "Order")
public class OrderEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Order_ID") public Long id;
  @Column(name = "Delivery_ID") public Long deliveryId;
  @Column(name = "Status") public String status = "PENDING_PAYMENT";
  @Column(name = "Order_Date") public Instant orderDate = Instant.now();
  @Column(name = "Buyer_Confirmed_At") public Instant buyerConfirmedAt;
  @Column(name = "Seller_Confirmed_At") public Instant sellerConfirmedAt;
}
