package com.marketplace;

import com.marketplace.db.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Read queries backing the WebMCP tools + catalog endpoints.
 *  IDs are Long in the DB; every id crossing this class's boundary is a String
 *  (JWT principals, tool args, JSON responses) and converted here. */
@Service
public class MarketplaceReads {

  private static final List<String> LIVE =
      List.of("open", "countered", "buyer_accepted", "seller_accepted");
  private static final int SEARCH_SCAN = 240;   // shared catalog is ~95k rows

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

  /** Parse an id from the wire; -1 (never a real key) on anything non-numeric
   *  so callers 404 instead of 500. */
  private static Long lid(String s) {
    if (s == null) return null;
    try { return Long.valueOf(s.trim()); } catch (NumberFormatException e) { return -1L; }
  }

  private static String imageOf(ProductEntity p) {
    if (p.imageUrl != null && !p.imageUrl.isBlank()) return p.imageUrl;
    return "https://picsum.photos/seed/prod" + p.id + "/500/600";   // fallback until URLs backfilled
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

  private String sellerName(Long sellerId) {
    if (sellerId == null) return "Shop";
    var s = sellers.findById(sellerId).orElse(null);
    if (s != null && s.tradingName != null && !s.tradingName.isBlank()) return s.tradingName;
    return users.findById(sellerId).map(u -> (u.firstName + " " + u.lastName).trim()).orElse("Shop " + sellerId);
  }

  private double sellerRating(Long sellerId) {
    return sellerId == null ? 0.0 : sellers.findById(sellerId).map(s -> s.rating).orElse(0.0);
  }

  public List<Map<String, Object>> searchProducts(String query, Double maxPrice, Double minRating) {
    return searchProducts(query, maxPrice, minRating, null, null);
  }

  public List<Map<String, Object>> searchProducts(String query, Double maxPrice, Double minRating, String category) {
    return searchProducts(query, maxPrice, minRating, category, null);
  }

  public List<Map<String, Object>> searchProducts(String query, Double maxPrice, Double minRating,
      String category, String sort) {
    var page = PageRequest.of(0, SEARCH_SCAN);
    var stream = products.findByNameContainingIgnoreCase(query == null ? "" : query, page).stream()
        .filter(p -> maxPrice == null || p.price <= maxPrice)
        .filter(p -> category == null || category.isBlank()
            || (p.category != null && category.equalsIgnoreCase(p.category)))
        .filter(p -> minRating == null || p.ratingAvg >= minRating);
    java.util.Comparator<ProductEntity> cmp = switch (sort == null ? "" : sort) {
      case "price_asc" -> java.util.Comparator.comparingDouble(x -> x.price);
      case "price_desc" -> java.util.Comparator.comparingDouble((ProductEntity x) -> x.price).reversed();
      case "rating" -> java.util.Comparator.comparingDouble((ProductEntity x) -> x.ratingAvg).reversed();
      case "sold" -> java.util.Comparator.comparingInt((ProductEntity x) -> x.soldCount).reversed();
      default -> null;
    };
    if (cmp != null) stream = stream.sorted(cmp);
    var list = stream.limit(60).toList();

    // batch the seller/user lookups (remote DB — avoid N+1)
    var sellerIds = list.stream().map(p -> p.sellerId).filter(java.util.Objects::nonNull).distinct().toList();
    var sMap = new java.util.HashMap<Long, SellerEntity>();
    sellers.findAllById(sellerIds).forEach(s -> sMap.put(s.id, s));
    var uMap = new java.util.HashMap<Long, UserEntity>();
    users.findAllById(sellerIds).forEach(u -> uMap.put(u.id, u));

    return list.stream().map(p -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("product_id", String.valueOf(p.id));
      m.put("name", p.name);
      m.put("price", p.price);
      m.put("category", p.category);
      m.put("image_url", imageOf(p));
      SellerEntity s = sMap.get(p.sellerId);
      UserEntity u = uMap.get(p.sellerId);
      String name = s != null && s.tradingName != null && !s.tradingName.isBlank() ? s.tradingName
          : u != null ? (u.firstName + " " + u.lastName).trim() : "Shop " + p.sellerId;
      m.put("seller_name", name);
      m.put("seller_rating", s != null ? s.rating : 0.0);
      storefront(m, p);
      return m;
    }).toList();
  }

  public Map<String, Object> getProduct(String productId, String buyerId) {
    ProductEntity p = products.findById(lid(productId)).orElse(null);
    if (p == null) return null;
    String open = buyerId == null ? null : negotiations
        .findByBuyerIdAndProductIdAndStatusIn(lid(buyerId), p.id, LIVE)
        .stream().findFirst().map(n -> String.valueOf(n.id)).orElse(null);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("product_id", String.valueOf(p.id));
    m.put("name", p.name);
    m.put("price", p.price);
    m.put("seller_name", sellerName(p.sellerId));
    m.put("seller_rating", sellerRating(p.sellerId));
    m.put("rating_avg", p.ratingAvg);
    m.put("review_count", p.ratingCount);
    m.put("remaining", p.remainings);
    m.put("open_negotiation_id", open);
    return m;
  }

  private List<Map<String, Object>> historyOf(Long negId) {
    return rounds.findByNegotiationIdOrderByRoundNumberAsc(negId).stream()
        .map(r -> Map.<String, Object>of(
            "round_number", r.roundNumber, "author", r.author,
            "proposed_price", r.proposedPrice, "message", r.messageContext == null ? "" : r.messageContext))
        .toList();
  }

  public List<Map<String, Object>> listBuyerNegotiations(String buyerId) {
    return negotiations.findByBuyerId(lid(buyerId)).stream()
        .map(n -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("negotiation_id", String.valueOf(n.id));
          m.put("product_id", String.valueOf(n.productId));
          m.put("name", products.findById(n.productId).map(p -> p.name).orElse("?"));
          m.put("status", n.status);
          m.put("current_round", n.currentRound);
          m.put("last_actor", n.lastActor);
          m.put("current_price", n.currentPrice);
          m.put("history", historyOf(n.id));
          return m;
        })
        .toList();
  }

  public List<Map<String, Object>> listIncomingOffers(String sellerId) {
    var productIds = products.findBySellerId(lid(sellerId)).stream().map(p -> p.id).toList();
    if (productIds.isEmpty()) return List.of();
    return negotiations.findByProductIdInAndStatusIn(productIds, List.of("countered")).stream()
        .filter(n -> "buyer".equals(n.lastActor))
        .map(n -> {
          ProductEntity p = products.findById(n.productId).orElseThrow();
          var cfg = sellerConfigs.findByProductId(n.productId).orElse(null);
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("negotiation_id", String.valueOf(n.id));
          m.put("product_id", String.valueOf(n.productId));
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
    NegotiationEntity n = negotiations.findById(lid(negotiationId)).orElse(null);
    if (n == null) return null;
    return Map.of("negotiation_id", negotiationId, "status", n.status,
        "current_round", n.currentRound, "history", historyOf(n.id));
  }

  public Map<String, Object> negotiationState(String negotiationId) {
    NegotiationEntity n = negotiations.findById(lid(negotiationId)).orElse(null);
    if (n == null) return Map.of("error", "NOT_FOUND");
    var rs = rounds.findByNegotiationIdOrderByRoundNumberAsc(n.id);
    Map<String, Object> sellerResponse = null;
    for (int i = rs.size() - 1; i >= 0; i--) {
      if ("seller".equals(rs.get(i).author)) {
        sellerResponse = Map.of("price", rs.get(i).proposedPrice,
            "message", rs.get(i).messageContext == null ? "" : rs.get(i).messageContext,
            "status", n.status);
        break;
      }
    }
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("negotiation_id", String.valueOf(n.id));
    m.put("product_id", String.valueOf(n.productId));
    m.put("status", n.status);
    m.put("current_round", n.currentRound);
    m.put("last_actor", n.lastActor);
    m.put("current_price", n.currentPrice);
    m.put("quantity", n.quantity);
    m.put("current_freebies_cost", n.currentFreebiesCost);
    m.put("current_free_shipping", n.currentFreeShipping);
    m.put("seller_response", sellerResponse);
    m.put("history", historyOf(n.id));
    return m;
  }

  /** Cheap items the seller of `productId` could throw in as freebies. */
  public List<Map<String, Object>> addonsFor(String productId) {
    ProductEntity target = products.findById(lid(productId)).orElse(null);
    if (target == null) return List.of();
    return products.findBySellerId(target.sellerId).stream()
        .filter(p -> !p.id.equals(target.id) && p.price <= 40)
        .limit(20)
        .map(p -> Map.<String, Object>of(
            "product_id", String.valueOf(p.id), "name", p.name, "value", p.minPrice))
        .toList();
  }

  public List<String> categories() {
    return products.findByNameContainingIgnoreCase("", PageRequest.of(0, 500)).stream()
        .map(p -> p.category).filter(c -> c != null && !c.isBlank())
        .distinct().sorted().toList();
  }

  /** Product detail: this DB has one listing per product id, so `sellers` has
   *  a single entry. Reviews come via the invoice-backed [Feedback] table. */
  public Map<String, Object> productDetail(String productId,
      Function<String, Double> avgRating,
      Function<String, List<Map<String, Object>>> reviews,
      Function<String, int[]> breakdown) {
    ProductEntity p = products.findById(lid(productId)).orElse(null);
    if (p == null) return null;

    String pid = String.valueOf(p.id);
    List<Map<String, Object>> revs = reviews.apply(pid);
    int[] bd = breakdown.apply(pid);

    Map<String, Object> shop = new LinkedHashMap<>();
    shop.put("product_id", pid);
    shop.put("seller_id", String.valueOf(p.sellerId));
    shop.put("seller_name", sellerName(p.sellerId));
    shop.put("seller_rating", sellerRating(p.sellerId));
    shop.put("price", p.price);
    shop.put("shipping_cost", p.shippingCost);
    shop.put("rating_avg", p.ratingAvg > 0 ? p.ratingAvg : avgRating.apply(pid));
    shop.put("rating_count", p.ratingCount > 0 ? p.ratingCount : revs.size());
    shop.put("rating_breakdown", bd);
    shop.put("reviews", revs);

    List<Map<String, Object>> merged = new ArrayList<>();
    for (Map<String, Object> r : revs) {
      Map<String, Object> t = new LinkedHashMap<>(r);
      t.put("seller_name", shop.get("seller_name"));
      merged.add(t);
    }

    double modelAvg = merged.isEmpty()
        ? (p.ratingAvg > 0 ? p.ratingAvg : 0.0)
        : Math.round(merged.stream().mapToInt(r -> ((Number) r.get("rating")).intValue())
            .average().orElse(0) * 10) / 10.0;

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("product_id", pid);
    m.put("name", p.name);
    m.put("description", p.description);
    m.put("category", p.category);
    m.put("price", p.price);
    m.put("image_url", imageOf(p));
    m.put("remaining", p.remainings);
    storefront(m, p);
    m.put("sellers", List.of(shop));
    m.put("avg_rating", modelAvg);
    m.put("rating_count", merged.size());
    m.put("rating_breakdown", bd);
    m.put("reviews", merged);
    return m;
  }
}
