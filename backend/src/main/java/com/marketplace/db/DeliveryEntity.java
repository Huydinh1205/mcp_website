package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

/** thanh's [Delivery]. Status CHECK: PENDING/IN_TRANSIT/OUT_FOR_DELIVERY/DELIVERED/FAILED. */
@Entity @Table(name = "Delivery")
public class DeliveryEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Delivery_ID") public Long id;
  @Column(name = "Tracking_Number") public String trackingNumber;
  @Column(name = "Address") public String address;
  @Column(name = "Status") public String status = "PENDING";
  @Column(name = "Estimated_Date") public Instant estimatedDate;
}
