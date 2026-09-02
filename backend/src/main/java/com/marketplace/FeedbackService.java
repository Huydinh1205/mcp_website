package com.marketplace;

import com.marketplace.db.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Post-purchase reviews on thanh's invoice-backed [Feedback] table
 * (PK = Product_ID + Invoice_ID). A review is only possible once the
 * negotiation is confirmed AND its order produced an invoice.
 */
@Service
public class FeedbackService {

  private final FeedbackRepo feedback;
  private final NegotiationRepo negotiations;
  private final InvoiceRepo invoices;
  private final UserRepo users;

  public FeedbackService(FeedbackRepo feedback, NegotiationRepo negotiations,
      InvoiceRepo invoices, UserRepo users) {
    this.feedback = feedback;
    this.negotiations = negotiations;
    this.invoices = invoices;
    this.users = users;
  }

  private static Long lid(String s) { return s == null ? null : Long.valueOf(s); }

  public record LeaveResult(boolean ok, String error) {}

  public LeaveResult leave(String productId, String negotiationId, String buyerId,
      int rating, String comment) {
    NegotiationEntity n = negotiations.findById(lid(negotiationId)).orElse(null);
    if (n == null) return new LeaveResult(false, "NEGOTIATION_NOT_FOUND");
    if (buyerId == null || !buyerId.equals(String.valueOf(n.buyerId)))
      return new LeaveResult(false, "FORBIDDEN");
    if (!"confirmed".equals(n.status)) return new LeaveResult(false, "NOT_PURCHASED");
    if (rating < 1 || rating > 5) return new LeaveResult(false, "BAD_RATING");
    if (n.orderId == null) return new LeaveResult(false, "NO_INVOICE");

    InvoiceEntity inv = invoices.findByOrderId(n.orderId).stream().findFirst().orElse(null);
    if (inv == null) return new LeaveResult(false, "NO_INVOICE");
    if (!feedback.findByInvoiceId(inv.id).isEmpty())
      return new LeaveResult(false, "ALREADY_REVIEWED");

    FeedbackEntity f = new FeedbackEntity();
    f.productId = n.productId;                 // trust the negotiation, not the body
    f.invoiceId = inv.id;
    f.ratingScore = rating;
    f.comment = comment;
    feedback.save(f);
    return new LeaveResult(true, null);
  }

  public double avgRating(String productId) {
    var rs = feedback.findByProductIdOrderByCreatedAtDesc(lid(productId));
    if (rs.isEmpty()) return 0.0;
    return Math.round(rs.stream().mapToInt(r -> r.ratingScore).average().orElse(0) * 10) / 10.0;
  }

  public List<Map<String, Object>> reviewsFor(String productId) {
    return feedback.findByProductIdOrderByCreatedAtDesc(lid(productId)).stream()
        .limit(50)
        .map(r -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("rating", r.ratingScore);
          m.put("comment", r.comment);
          m.put("reviewer", reviewerName(r.invoiceId));
          m.put("verified", true);            // invoice-backed
          m.put("created_at", r.createdAt);
          return m;
        })
        .toList();
  }

  private String reviewerName(Long invoiceId) {
    InvoiceEntity inv = invoices.findById(invoiceId).orElse(null);
    if (inv == null) return "Anonymous";
    return users.findById(inv.buyerId)
        .map(u -> (u.firstName + " " + (u.lastName == null ? "" : u.lastName.charAt(0) + ".")).trim())
        .orElse("Anonymous");
  }

  /** counts per star 5..1 */
  public int[] breakdown(String productId) {
    int[] b = new int[5];
    for (var r : feedback.findByProductIdOrderByCreatedAtDesc(lid(productId))) {
      int i = Math.min(5, Math.max(1, r.ratingScore));
      b[5 - i]++;
    }
    return b;
  }
}
