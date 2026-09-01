package com.marketplace.db;

import jakarta.persistence.*;

@Entity @Table(name = "seller_ai_configs")
public class SellerAiConfigEntity {
  @Id @Column(name = "agent_id", length = 64) public String agentId;
  @Column(name = "auto_accept_price") public double autoAcceptPrice;
  @Column(name = "max_discount_step") public double maxDiscountStep;
  @Column(name = "product_id", length = 64) public String productId;
}
