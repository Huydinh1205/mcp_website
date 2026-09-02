package com.marketplace;

import com.marketplace.db.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Post-purchase reviews. Only allowed once a negotiation is confirmed. */
@Service
public class FeedbackService {

  private final FeedbackRepo feedback;
  private final NegotiationRepo negotiations;

  public FeedbackService(FeedbackRepo feedback, NegotiationRepo negotiations) {
    this.feedback = feedback;
    this.negotiations = negotiations;
  }

  public record LeaveResult(boolean ok, String error) {}

  public LeaveResult leave(String productId, String negotiationId, String buyerId,
      int rating, String comment) {
    NegotiationEntity n = negotiations.findById(negotiationId).orElse(null);
    if (n == null) return new LeaveResult(false, "NEGOTIATION_NOT_FOUND");
    // IDOR: the reviewer must be the buyer on this negotiation.
    if (buyerId == null || !buyerId.equals(n.nationalId))
      return new LeaveResult(false, "FORBIDDEN");
    if (!"confirmed".equals(n.status)) return new LeaveResult(false, "NOT_PURCHASED");
    if (rating < 1 || rating > 5) return new LeaveResult(false, "BAD_RATING");
    if (!feedback.findByNegotiationId(negotiationId).isEmpty())
      return new LeaveResult(false, "ALREADY_REVIEWED");

    FeedbackEntity f = new FeedbackEntity();
    f.feedbackId = UUID.randomUUID().toString().replace("-", "");
    // Trust the negotiation, not the request body.
    f.productId = n.productId;
    f.negotiationId = negotiationId;
    f.buyerId = n.nationalId;
    f.ratingScore = rating;
    f.comment = comment;
    feedback.save(f);
    return new LeaveResult(true, null);
  }

  public double avgRating(String productId) {
    var rs = feedback.findByProductIdOrderByCreatedAtDesc(productId);
    if (rs.isEmpty()) return 0.0;
    return Math.round(rs.stream().mapToInt(r -> r.ratingScore).average().orElse(0) * 10) / 10.0;
  }

  public List<Map<String, Object>> reviewsFor(String productId) {
    return feedback.findByProductIdOrderByCreatedAtDesc(productId).stream()
        .limit(50)
        .map(r -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("rating", r.ratingScore);
          m.put("comment", r.comment);
          m.put("reviewer", r.reviewerName == null ? "Anonymous" : r.reviewerName);
          m.put("verified", r.verified);
          m.put("created_at", r.createdAt);
          return m;
        })
        .toList();
  }

  /** counts per star 5..1 */
  public int[] breakdown(String productId) {
    int[] b = new int[5];
    for (var r : feedback.findByProductIdOrderByCreatedAtDesc(productId)) {
      int i = Math.min(5, Math.max(1, r.ratingScore));
      b[5 - i]++;
    }
    return b;
  }
}
