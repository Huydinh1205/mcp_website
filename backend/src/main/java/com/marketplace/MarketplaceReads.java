package com.marketplace;

import com.marketplace.db.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/** Read queries backing the WebMCP tools + directory endpoints. */
@Service
public class MarketplaceReads {

  private static final List<String> LIVE =
      List.of("open", "countered", "buyer_accepted", "seller_accepted");

  private final ProductRepo products;
  private final SellerRepo sellers;
  private final BuyerRepo buyers;
  private final UserRepo users;
  private final SellerConfigRepo sellerConfigs;
  private final BuyerConfigRepo buyerConfigs;
  private final NegotiationRepo negotiations;
  private final NegotiationRoundRepo rounds;

  public MarketplaceReads(ProductRepo products, SellerRepo sellers, BuyerRepo buyers, UserRepo users,
      SellerConfigRepo sellerConfigs, BuyerConfigRepo buyerConfigs,
      NegotiationRepo negotiations, NegotiationRoundRepo rounds) {
    this.products = products;
    this.sellers = sellers;
    this.buyers = buyers;
    this.users = users;
    this.sellerConfigs = sellerConfigs;
    this.buyerConfigs = buyerConfigs;
    this.negotiations = negotiations;
    this.rounds = rounds;
  }

  private void storefront(Map<String, Object> m, ProductEntity p) {
    m.put("compare_at_price", p.compareAtPrice);
    m.put("rating_avg", p.ratingAvg);
    m.put("rating_count", p.ratingCount);
    m.put("sold_count", p.soldCount);
    m.put("free_shipping", p.shippingCost <= 0.0);
    int pct = p.compareAtPrice != null && p.compareAtPrice > p.price
        ? (int) Math.round((1 - p.price / p.compareAtPrice) * 100) : 0;
    m.put("discount_pct", pct);
  }

  private String sellerName(String sellerId) {
    return users.findById(sellerId)
        .map(u -> u.firstName + " " + u.lastName).orElse(sellerId);
  }

  public List<Map<String, Object>> searchProducts(String query, Double maxPrice, Double minRating) {
    return searchProducts(query, maxPrice, minRating, null);
  }

  public List<Map<String, Object>> searchProducts(String query, Double maxPrice, Double minRating, String category) {
    return searchProducts(query, maxPrice, minRating, category, null);
  }

  public List<Map<String, Object>> searchProducts(String query, Double maxPrice, Double minRating,
      String category, String sort) {
    var stream = products.findByNameContainingIgnoreCase(query == null ? "" : query).stream()
        .filter(p -> maxPrice == null || p.price <= maxPrice)
        .filter(p -> category == null || category.isBlank() || category.equalsIgnoreCase(p.category))
        .filter(p -> {
          if (minRating == null) return true;
          return p.ratingAvg >= minRating;
        });
    java.util.Comparator<ProductEntity> cmp = switch (sort == null ? "" : sort) {
      case "price_asc" -> java.util.Comparator.comparingDouble(x -> x.price);
      case "price_desc" -> java.util.Comparator.comparingDouble((ProductEntity x) -> x.price).reversed();
      case "rating" -> java.util.Comparator.comparingDouble((ProductEntity x) -> x.ratingAvg).reversed();
      case "sold" -> java.util.Comparator.comparingInt((ProductEntity x) -> x.soldCount).reversed();
      default -> null;
    };
    if (cmp != null) stream = stream.sorted(cmp);
    return stream.map(p -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("product_id", p.productId);
      m.put("name", p.name);
      m.put("price", p.price);
      m.put("category", p.category);
      m.put("image_url", p.imageUrl);
      m.put("seller_name", sellerName(p.sellerId));
      m.put("seller_rating", sellers.findById(p.sellerId).map(x -> x.rating).orElse(0.0));
      storefront(m, p);
      return m;
    }).limit(60).toList();
  }

  public Map<String, Object> getProduct(String productId, String buyerId) {
    ProductEntity p = products.findById(productId).orElse(null);
    if (p == null) return null;
    var open = negotiations
        .findByNationalIdAndProductIdAndStatusIn(buyerId, productId, LIVE)
        .stream().findFirst().map(n -> n.negotiationId).orElse(null);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("product_id", p.productId);
    m.put("name", p.name);
    m.put("price", p.price);
    m.put("seller_name", sellerName(p.sellerId));
    m.put("seller_rating", sellers.findById(p.sellerId).map(s -> s.rating).orElse(0.0));
    m.put("rating_avg", p.ratingAvg);
    m.put("review_count", p.ratingCount);
    m.put("remaining", p.remainings);
    m.put("open_negotiation_id", open);
    return m;
  }

  public List<Map<String, Object>> listBuyerNegotiations(String buyerId) {
    return negotiations.findByNationalId(buyerId).stream()
        .map(n -> {
          var hist = rounds.findByNegotiationIdOrderByRoundNumberAsc(n.negotiationId).stream()
              .map(r -> Map.<String, Object>of(
                  "round_number", r.roundNumber, "author", r.author,
                  "proposed_price", r.proposedPrice, "message", r.messageContext))
              .toList();
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("negotiation_id", n.negotiationId);
          m.put("product_id", n.productId);
          m.put("name", products.findById(n.productId).map(p -> p.name).orElse("?"));
          m.put("status", n.status);
          m.put("current_round", n.currentRound);
          m.put("last_actor", n.lastActor);
          m.put("current_price", n.currentPrice);
          m.put("history", hist);
          return m;
        })
        .toList();
  }

  public List<Map<String, Object>> listIncomingOffers(String sellerId) {
    var productIds = products.findBySellerId(sellerId).stream().map(p -> p.productId).toList();
    if (productIds.isEmpty()) return List.of();
    return negotiations.findByProductIdInAndStatusIn(productIds, List.of("countered")).stream()
        .filter(n -> "buyer".equals(n.lastActor))
        .map(n -> {
          ProductEntity p = products.findById(n.productId).orElseThrow();
          var cfg = sellerConfigs.findByProductId(n.productId).orElse(null);
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("negotiation_id", n.negotiationId);
          m.put("product_id", n.productId);
          m.put("name", p.name);
          m.put("status", n.status);
          m.put("current_round", n.currentRound);
          m.put("last_actor", n.lastActor);
          m.put("current_price", n.currentPrice);
          m.put("min_price", p.minPrice);
          m.put("auto_accept_price", cfg != null ? cfg.autoAcceptPrice : p.minPrice);
          m.put("max_discount_step", cfg != null ? cfg.maxDiscountStep : p.price);
          return m;
        })
        .toList();
  }

  public Map<String, Object> getOfferHistory(String negotiationId) {
    NegotiationEntity n = negotiations.findById(negotiationId).orElse(null);
    if (n == null) return null;
    var hist = rounds.findByNegotiationIdOrderByRoundNumberAsc(negotiationId).stream()
        .map(r -> Map.<String, Object>of(
            "round_number", r.roundNumber, "author", r.author,
            "proposed_price", r.proposedPrice, "message", r.messageContext))
        .toList();
    return Map.of("negotiation_id", negotiationId, "status", n.status,
        "current_round", n.currentRound, "history", hist);
  }

  public Map<String, Object> negotiationState(String negotiationId) {
    NegotiationEntity n = negotiations.findById(negotiationId).orElse(null);
    if (n == null) return Map.of("error", "NOT_FOUND");
    var rs = rounds.findByNegotiationIdOrderByRoundNumberAsc(negotiationId);
    Map<String, Object> sellerResponse = null;
    for (int i = rs.size() - 1; i >= 0; i--) {
      if ("seller".equals(rs.get(i).author)) {
        sellerResponse = Map.of("price", rs.get(i).proposedPrice,
            "message", rs.get(i).messageContext, "status", n.status);
        break;
      }
    }
    var hist = rs.stream().map(r -> Map.<String, Object>of(
        "round_number", r.roundNumber, "author", r.author,
        "proposed_price", r.proposedPrice, "message", r.messageContext)).toList();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("negotiation_id", n.negotiationId);
    m.put("product_id", n.productId);
    m.put("status", n.status);
    m.put("current_round", n.currentRound);
    m.put("last_actor", n.lastActor);
    m.put("current_price", n.currentPrice);
    m.put("quantity", n.quantity);
    m.put("current_freebies_cost", n.currentFreebiesCost);
    m.put("current_free_shipping", n.currentFreeShipping);
    m.put("seller_response", sellerResponse);
    m.put("history", hist);
    return m;
  }



  /** Cheap items the seller of `productId` could throw in as freebies. */
  public List<Map<String, Object>> addonsFor(String productId) {
    ProductEntity target = products.findById(productId).orElse(null);
    if (target == null) return List.of();
    return products.findBySellerId(target.sellerId).stream()
        .filter(p -> !p.productId.equals(productId) && p.price <= 40)
        .map(p -> Map.<String, Object>of(
            "product_id", p.productId, "name", p.name, "value", p.minPrice))
        .toList();
  }

  public List<String> categories() {
    return products.findAll().stream()
        .map(p -> p.category).filter(c -> c != null && !c.isBlank())
        .distinct().sorted().toList();
  }

  /**
   * Product detail for the human catalog. Every seller carrying this name is a
   * separate listing with its OWN reviews: each entry in `sellers` carries that
   * shop's rating_avg / rating_count / rating_breakdown / reviews. The top-level
   * `reviews` / `avg_rating` / `rating_breakdown` merge across all shops, and
   * each merged review is tagged with `seller_name`.
   */
  public Map<String, Object> productDetail(String productId,
      Function<String, Double> avgRating,
      Function<String, List<Map<String, Object>>> reviews,
      Function<String, int[]> breakdown) {
    ProductEntity p = products.findById(productId).orElse(null);
    if (p == null) return null;

    var listings = products.findByNameContainingIgnoreCase(p.name).stream()
        .filter(x -> x.name.equalsIgnoreCase(p.name))
        .toList();

    List<Map<String, Object>> sellerRows = new ArrayList<>();
    List<Map<String, Object>> mergedReviews = new ArrayList<>();
    int[] mergedBreakdown = new int[5];

    for (ProductEntity x : listings) {
      String shop = sellerName(x.sellerId);
      List<Map<String, Object>> xReviews = reviews.apply(x.productId);
      int[] xBreakdown = breakdown.apply(x.productId);

      Map<String, Object> row = new LinkedHashMap<>();
      row.put("product_id", x.productId);
      row.put("seller_id", x.sellerId);
      row.put("seller_name", shop);
      row.put("seller_rating", sellers.findById(x.sellerId).map(s -> s.rating).orElse(0.0));
      row.put("price", x.price);
      row.put("shipping_cost", x.shippingCost);
      row.put("rating_avg", x.ratingAvg > 0 ? x.ratingAvg : avgRating.apply(x.productId));
      row.put("rating_count", x.ratingCount > 0 ? x.ratingCount : xReviews.size());
      row.put("rating_breakdown", xBreakdown);
      row.put("reviews", xReviews);
      sellerRows.add(row);

      for (int i = 0; i < 5; i++) mergedBreakdown[i] += xBreakdown[i];
      for (Map<String, Object> r : xReviews) {
        Map<String, Object> tagged = new LinkedHashMap<>(r);
        tagged.put("seller_name", shop);
        mergedReviews.add(tagged);
      }
    }
    mergedReviews.sort((a, b) ->
        String.valueOf(b.get("created_at")).compareTo(String.valueOf(a.get("created_at"))));

    double modelAvg = mergedReviews.isEmpty()
        ? (p.ratingAvg > 0 ? p.ratingAvg : 0.0)
        : Math.round(mergedReviews.stream()
            .mapToInt(r -> ((Number) r.get("rating")).intValue()).average().orElse(0) * 10) / 10.0;

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("product_id", p.productId);
    m.put("name", p.name);
    m.put("category", p.category);
    m.put("price", p.price);
    m.put("image_url", p.imageUrl);
    storefront(m, p);
    m.put("sellers", sellerRows);
    m.put("avg_rating", modelAvg);
    m.put("rating_count", mergedReviews.size());
    m.put("rating_breakdown", mergedBreakdown);
    m.put("reviews", mergedReviews);
    return m;
  }
}
