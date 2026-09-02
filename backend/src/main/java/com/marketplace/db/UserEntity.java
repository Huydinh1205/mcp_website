package com.marketplace.db;

import jakarta.persistence.*;

/** Maps the shared schema's [User] table. PK is bigint IDENTITY. */
@Entity @Table(name = "User")
public class UserEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "User_ID") public Long id;
  @Column(name = "First_Name") public String firstName;
  @Column(name = "Last_Name") public String lastName;
  @Column(name = "Email") public String email;
  @Column(name = "Phone_Number") public String phoneNumber;
  @Column(name = "Password_Hash") public String passwordHash;
  @Column(name = "Role") public String role;            // "buyer" | "seller"
}
