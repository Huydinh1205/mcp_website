package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

/** App-owned coupon table [App_Coupon] (thanh's Discount has no code / target). */
@Entity @Table(name = "App_Coupon")
public class DiscountEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Coupon_ID") public Long id;
  @Column(name = "Code", length = 40) public String code;
  @Column(name = "Label") public String label;
  @Column(name = "Percent_Off") public Double percent;   // 0..1
  @Column(name = "Amount") public Double amount;          // flat
  @Column(name = "Product_ID") public Long productId;     // nullable
  @Column(name = "Seller_ID") public Long sellerId;       // nullable
  @Column(name = "Start_Date") public Instant startDate;
  @Column(name = "End_Date") public Instant endDate;
}
