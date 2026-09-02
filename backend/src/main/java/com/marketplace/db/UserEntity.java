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
  @Column(name = "Street_Address") public String streetAddress;
  @Column(name = "Suburb") public String suburb;
  @Column(name = "State") public String state;          // CHK: NSW/VIC/QLD/WA/SA/TAS/ACT/NT or null
  @Column(name = "Postcode") public String postcode;    // CHK: 4 digits or null

  /** One-line shipping address, or null if nothing on file. */
  public String fullAddress() {
    if (streetAddress == null || streetAddress.isBlank()) return null;
    StringBuilder b = new StringBuilder(streetAddress.trim());
    if (suburb != null && !suburb.isBlank()) b.append(", ").append(suburb.trim());
    if (state != null && !state.isBlank()) b.append(" ").append(state.trim());
    if (postcode != null && !postcode.isBlank()) b.append(" ").append(postcode.trim());
    return b.toString();
  }
}
