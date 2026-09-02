package com.marketplace.db;

import jakarta.persistence.*;

/** Shared schema [Product]: Product_ID is bigint IDENTITY. URL is the image.
 *  Category / Shipping_Cost / Compare_At_Price / Rating_* / Sold_Count were
 *  added for this app (see db-adapt-to-shared.sql). */
@Entity @Table(name = "Product")
public class ProductEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Product_ID") public Long id;
  @Column(name = "Seller_ID") public Long sellerId;
  @Column(name = "Name") public String name;
  @Column(name = "Description") public String description;
  @Column(name = "Price") public double price;
  @Column(name = "Min_Price") public double minPrice;
  @Column(name = "Gap") public Double gap;
  @Column(name = "Remainings") public int remainings;
  @Column(name = "URL") public String imageUrl;
  @Column(name = "Category") public String category;
  @Column(name = "Shipping_Cost") public double shippingCost = 5;
  @Column(name = "Compare_At_Price") public Double compareAtPrice;
  @Column(name = "Rating_Avg") public double ratingAvg = 0;
  @Column(name = "Rating_Count") public int ratingCount = 0;
  @Column(name = "Sold_Count") public int soldCount = 0;
}
