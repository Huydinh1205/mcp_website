package com.marketplace.db;

import jakarta.persistence.*;

/** Shared schema [Seller_AI_Config]: Agent_ID is bigint IDENTITY, one per product. */
@Entity @Table(name = "Seller_AI_Config")
public class SellerAiConfigEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Agent_ID") public Long agentId;
  @Column(name = "Product_ID") public Long productId;
  @Column(name = "Auto_Accept_Price") public double autoAcceptPrice;
  @Column(name = "Max_Discount_Step") public double maxDiscountStep;
  @Column(name = "Strategy_Mode") public String strategyMode = "BALANCED";
  @Column(name = "Is_Enabled") public boolean isEnabled = true;
}
