package com.marketplace.db;

import jakarta.persistence.*;

@Entity @Table(name = "users")
public class UserEntity {
  @Id @Column(name = "national_id", length = 64) public String nationalId;
  @Column(name = "first_name") public String firstName;
  @Column(name = "last_name") public String lastName;
  @Column public String email;
  @Column public String role;                          // "buyer" | "seller"
  @Column(name = "password_hash") public String passwordHash;
}
