package com.marketplace.db;

import jakarta.persistence.*;

@Entity @Table(name = "buyers")
public class BuyerEntity {
  @Id @Column(name = "national_id", length = 64) public String nationalId;
  @Column public String interest;
}
