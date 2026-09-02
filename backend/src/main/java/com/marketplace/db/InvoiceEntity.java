package com.marketplace.db;

import jakarta.persistence.*;
import java.time.Instant;

/** thanh's [Invoice] — created when a negotiation is fully confirmed. */
@Entity @Table(name = "Invoice")
public class InvoiceEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Invoice_ID") public Long id;
  @Column(name = "Order_ID") public Long orderId;
  @Column(name = "Buyer_ID") public Long buyerId;
  @Column(name = "Payment_ID") public int paymentId = 1;   // 1 = CreditCard
  @Column(name = "Quantity") public int quantity = 1;
  @Column(name = "Total_Cost") public double totalCost;
  @Column(name = "Exporting_Date") public Instant exportingDate = Instant.now();
}
