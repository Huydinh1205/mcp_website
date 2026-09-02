package com.marketplace.web;

import com.marketplace.FeedbackService;
import com.marketplace.MarketplaceReads;
import com.marketplace.auth.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Human-facing catalog: browse / search / product detail / reviews. */
@RestController
public class CatalogController {

  private final MarketplaceReads reads;
  private final FeedbackService feedback;

  public CatalogController(MarketplaceReads reads, FeedbackService feedback) {
    this.reads = reads;
    this.feedback = feedback;
  }

  @GetMapping("/api/products")
  public List<Map<String, Object>> products(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Double maxPrice,
      @RequestParam(required = false) Double minRating,
      @RequestParam(required = false) String sort) {
    return reads.searchProducts(q, maxPrice, minRating, category, sort);
  }

  @GetMapping("/api/categories")
  public List<String> categories() {
    return reads.categories();
  }

  @GetMapping("/api/shops/{id}")
  public ResponseEntity<?> shop(@PathVariable String id) {
    var s = reads.shopDetail(id);
    if (s == null) return ResponseEntity.status(404).body(Map.of("error", "NOT_FOUND"));
    return ResponseEntity.ok(s);
  }

  @GetMapping("/api/products/{id}")
  public ResponseEntity<?> product(@PathVariable String id) {
    var d = reads.productDetail(id, feedback::avgRating, feedback::reviewsFor, feedback::breakdown);
    if (d == null) return ResponseEntity.status(404).body(Map.of("error", "NOT_FOUND"));
    return ResponseEntity.ok(d);
  }

  @PostMapping("/api/feedback")
  public ResponseEntity<?> leaveFeedback(@RequestBody Map<String, Object> body) {
    var r = feedback.leave(
        String.valueOf(body.get("product_id")),
        String.valueOf(body.get("negotiation_id")),
        CurrentUser.id(),
        (int) Math.round(Double.parseDouble(String.valueOf(body.getOrDefault("rating", 0)))),
        body.get("comment") == null ? null : String.valueOf(body.get("comment")));
    return r.ok() ? ResponseEntity.ok(Map.of("ok", true))
        : ResponseEntity.badRequest().body(Map.of("error", r.error()));
  }
}
