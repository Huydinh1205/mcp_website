package com.marketplace;

import com.marketplace.db.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  private String sellerName(String sellerId) {
    return users.findById(sellerId)
        .map(u -> u.firstName + " " + u.lastName).orElse(sellerId);
  }

  public List<Map<String, Object>> searchProducts(String query, Double maxPrice, Double minRating) {
    return searchProducts(query, maxPrice, minRating, null);
  }

  public List<Map<String, Object>> searchProducts(String query, Double maxPrice, Double minRating, String category) {
    return products.findByNameContainingIgnoreCase(query == null ? "" : query).stream()
        .filter(p -> maxPrice == null || p.price <= maxPrice)
        .filter(p -> category == null || category.isBlank() || category.equalsIgnoreCase(p.category))
        .filter(p -> {
          if (minRating == null) return true;
          return sellers.findById(p.sellerId).map(s -> s.rating >= minRating).orElse(false);
        })
        .map(p -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("product_id", p.productId);
          m.put("name", p.name);
          m.put("price", p.price);
          m.put("category", p.category);
          m.put("image_url", p.imageUrl);
          m.put("seller_name", sellerName(p.sellerId));
          m.put("seller_rating", sellers.findById(p.sellerId).map(x -> x.rating).orElse(0.0));
          return m;
        })
        .limit(20)
        .toList();
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

  /** Product detail for the human catalog: every seller carrying this name + reviews. */
  public Map<String, Object> productDetail(String productId, java.util.function.Function<String, Double> avgRating,
      java.util.function.Function<String, List<Map<String, Object>>> reviews) {
    ProductEntity p = products.findById(productId).orElse(null);
    if (p == null) return null;
    var siblings = products.findByNameContainingIgnoreCase(p.name).stream()
        .filter(x -> x.name.equalsIgnoreCase(p.name))
        .map(x -> Map.<String, Object>of(
            "product_id", x.productId,
            "seller_id", x.sellerId,
            "seller_name", sellerName(x.sellerId),
            "seller_rating", sellers.findById(x.sellerId).map(s -> s.rating).orElse(0.0),
            "price", x.price,
            "shipping_cost", x.shippingCost))
        .toList();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("product_id", p.productId);
    m.put("name", p.name);
    m.put("category", p.category);
    m.put("price", p.price);
    m.put("image_url", p.imageUrl);
    m.put("sellers", siblings);
    m.put("avg_rating", avgRating.apply(p.productId));
    m.put("reviews", reviews.apply(p.productId));
    return m;
  }
}
