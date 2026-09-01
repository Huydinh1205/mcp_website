package com.marketplace;

import com.marketplace.db.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seller coupons. Kept OUT of applyTurn (Constitution IV untouched): the coupon
 * is attached to a negotiation and applied to the price at confirmation time.
 * The negotiated (base) price is what the two agents agree on; the coupon is an
 * extra reduction the seller published.
 */
@Service
public class DiscountService {

  private final DiscountRepo discounts;
  private final AppliedDiscountRepo applied;
  private final ProductRepo products;

  public DiscountService(DiscountRepo discounts, AppliedDiscountRepo applied, ProductRepo products) {
    this.discounts = discounts;
    this.applied = applied;
    this.products = products;
  }

  // ---- pure -------------------------------------------------------------

  /** Amount to subtract from basePrice for this discount, clamped to [0, basePrice]. */
  public double discountAmount(double basePrice, DiscountEntity d) {
    double raw = d.percent != null ? basePrice * d.percent
        : d.amount != null ? d.amount
        : 0.0;
    return Math.max(0.0, Math.min(basePrice, raw));
  }

  public boolean isActive(DiscountEntity d, Instant now) {
    return d.startDate != null && d.endDate != null
        && !now.isBefore(d.startDate) && !now.isAfter(d.endDate);
  }

  public boolean appliesTo(DiscountEntity d, String productId, String sellerId) {
    if (d.productId != null) return d.productId.equals(productId);
    if (d.sellerId != null) return d.sellerId.equals(sellerId);
    return true; // global
  }

  // ---- persisted ------------------------------------------------------

  public List<Map<String, Object>> couponsFor(String productId) {
    ProductEntity p = products.findById(productId).orElse(null);
    if (p == null) return List.of();
    Instant now = Instant.now();
    return discounts.findAll().stream()
        .filter(d -> isActive(d, now) && appliesTo(d, productId, p.sellerId))
        .map(d -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("code", d.code);
          m.put("label", d.label);
          m.put("percent", d.percent);
          m.put("amount", d.amount);
          return m;
        })
        .toList();
  }

  public record ApplyResult(boolean ok, String error, double basePrice, double effectivePrice,
      double discount, String code) {}

  @Transactional
  public ApplyResult applyCoupon(String negotiationId, String code, double basePrice, String productId) {
    ProductEntity p = products.findById(productId).orElse(null);
    if (p == null) return new ApplyResult(false, "PRODUCT_NOT_FOUND", basePrice, basePrice, 0, code);
    DiscountEntity d = discounts.findByCodeIgnoreCase(code == null ? "" : code.trim()).orElse(null);
    if (d == null) return new ApplyResult(false, "INVALID_CODE", basePrice, basePrice, 0, code);
    if (!isActive(d, Instant.now())) return new ApplyResult(false, "EXPIRED", basePrice, basePrice, 0, code);
    if (!appliesTo(d, productId, p.sellerId))
      return new ApplyResult(false, "NOT_APPLICABLE", basePrice, basePrice, 0, code);

    boolean already = applied.findByNegotiationId(negotiationId).stream()
        .anyMatch(a -> a.discountId.equals(d.discountId));
    if (!already) {
      AppliedDiscountEntity a = new AppliedDiscountEntity();
      a.negotiationId = negotiationId;
      a.discountId = d.discountId;
      applied.save(a);
    }
    double amt = discountAmount(basePrice, d);
    return new ApplyResult(true, null, basePrice, round2(basePrice - amt), round2(amt), d.code);
  }

  /** Total discount recorded on a negotiation, given its base price. */
  public double totalDiscountFor(String negotiationId, double basePrice) {
    double total = 0;
    for (AppliedDiscountEntity a : applied.findByNegotiationId(negotiationId)) {
      DiscountEntity d = discounts.findById(a.discountId).orElse(null);
      if (d != null) total += discountAmount(basePrice, d);
    }
    return round2(Math.min(basePrice, total));
  }

  public String seedDiscount(String code, String label, Double percent, Double amount,
      String productId, String sellerId, Instant start, Instant end) {
    DiscountEntity d = new DiscountEntity();
    d.discountId = UUID.randomUUID().toString().replace("-", "");
    d.code = code; d.label = label; d.percent = percent; d.amount = amount;
    d.productId = productId; d.sellerId = sellerId; d.startDate = start; d.endDate = end;
    return discounts.save(d).discountId;
  }

  private static double round2(double n) { return Math.round(n * 100.0) / 100.0; }
}
