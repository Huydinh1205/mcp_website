package com.marketplace.db;

import jakarta.persistence.*;

@Entity @Table(name = "products")
public class ProductEntity {
  @Id @Column(name = "product_id", length = 64) public String productId;
  @Column public String name;
  @Column public double price;
  @Column(name = "min_price") public double minPrice;
  @Column public double gap;
  @Column public int remainings;
  @Column public String category;
  @Column(name = "shipping_cost") public double shippingCost = 5;
  @Column(name = "image_url") public String imageUrl;
  @Column(name = "seller_id", length = 64) public String sellerId;
}
