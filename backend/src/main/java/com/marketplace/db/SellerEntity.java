package com.marketplace.db;

import jakarta.persistence.*;

/** Shared schema [Seller]: PK is User_ID (shared with [User]). */
@Entity @Table(name = "Seller")
public class SellerEntity {
  @Id @Column(name = "User_ID") public Long id;
  @Column(name = "ABN") public String abn;
  @Column(name = "Trading_Name") public String tradingName;
  @Column(name = "Rating") public double rating = 5.0;
  @Column(name = "Total_Ratings") public int totalRatings = 0;
}
