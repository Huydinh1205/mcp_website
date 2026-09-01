package com.marketplace.db;

import jakarta.persistence.*;

@Entity @Table(name = "sellers")
public class SellerEntity {
  @Id @Column(name = "national_id", length = 64) public String nationalId;
  @Column public double rating;
}
