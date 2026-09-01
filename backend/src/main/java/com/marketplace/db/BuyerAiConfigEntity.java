package com.marketplace.db;

import jakarta.persistence.*;

@Entity @Table(name = "buyer_ai_configs")
public class BuyerAiConfigEntity {
  @Id @Column(name = "buyer_agent_id", length = 64) public String buyerAgentId;
  @Column(name = "max_budget") public double maxBudget;
  @Column(name = "target_price") public double targetPrice;
  @Column(name = "min_seller_rating") public double minSellerRating;
  @Column(name = "is_active") public boolean isActive = true;
  @Column public String style = "fair";
  @Column(name = "national_id", length = 64) public String nationalId;
}
