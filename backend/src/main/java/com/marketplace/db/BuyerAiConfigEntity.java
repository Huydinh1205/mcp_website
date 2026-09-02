package com.marketplace.db;

import jakarta.persistence.*;

/** Shared schema [Buyer_AI_Config]: Buyer_Agent_ID is bigint IDENTITY. */
@Entity @Table(name = "Buyer_AI_Config")
public class BuyerAiConfigEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Buyer_Agent_ID") public Long buyerAgentId;
  @Column(name = "Buyer_ID") public Long buyerId;
  @Column(name = "Max_Budget") public double maxBudget;
  @Column(name = "Target_Price") public double targetPrice;
  @Column(name = "Min_Seller_Rating") public double minSellerRating;
  @Column(name = "Style") public String style = "MODERATE";
}
