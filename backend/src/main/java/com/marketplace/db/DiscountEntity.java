package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "discounts")
public class DiscountEntity {
  @Id @Column(name = "discount_id", length = 64) public String discountId;
  @Column(length = 40) public String code;
  @Column public String label;
  @Column(name = "percent_off") public Double percent; // 0..1
  @Column public Double amount;    // flat
  @Column(name = "product_id", length = 64) public String productId;  // nullable
  @Column(name = "seller_id", length = 64) public String sellerId;    // nullable
  @Column(name = "start_date") public Instant startDate;
  @Column(name = "end_date") public Instant endDate;
}
