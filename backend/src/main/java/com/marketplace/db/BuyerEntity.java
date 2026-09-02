package com.marketplace.db;

import jakarta.persistence.*;

/** Shared schema [Buyer]: PK is User_ID (shared with [User], not generated). */
@Entity @Table(name = "Buyer")
public class BuyerEntity {
  @Id @Column(name = "User_ID") public Long id;
  @Column(name = "Interest") public String interest;
  @Column(name = "History_Summary") public String historySummary;
}
