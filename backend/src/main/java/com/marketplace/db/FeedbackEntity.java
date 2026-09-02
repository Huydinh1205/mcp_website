package com.marketplace.db;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** thanh's [Feedback] — invoice-backed review, composite PK (Product_ID, Invoice_ID). */
@Entity @Table(name = "Feedback")
@IdClass(FeedbackEntity.Key.class)
public class FeedbackEntity {
  @Id @Column(name = "Product_ID") public Long productId;
  @Id @Column(name = "Invoice_ID") public Long invoiceId;
  @Column(name = "Rating_Score") public int ratingScore;
  @Column(name = "Comment") public String comment;
  @Column(name = "Created_At") public Instant createdAt = Instant.now();

  public static class Key implements Serializable {
    public Long productId;
    public Long invoiceId;
    public Key() {}
    public Key(Long productId, Long invoiceId) { this.productId = productId; this.invoiceId = invoiceId; }
    @Override public boolean equals(Object o) {
      if (!(o instanceof Key k)) return false;
      return Objects.equals(productId, k.productId) && Objects.equals(invoiceId, k.invoiceId);
    }
    @Override public int hashCode() { return Objects.hash(productId, invoiceId); }
  }
}
