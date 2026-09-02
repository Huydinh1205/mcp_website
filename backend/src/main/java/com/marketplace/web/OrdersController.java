package com.marketplace.web;

import com.marketplace.OrderService;
import com.marketplace.TokenService;
import com.marketplace.auth.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrdersController {

  private final TokenService tokens;
  private final OrderService orders;

  public OrdersController(TokenService tokens, OrderService orders) {
    this.tokens = tokens;
    this.orders = orders;
  }

  @PostMapping("/api/orders/confirm")
  public ResponseEntity<?> confirm(@RequestBody Map<String, Object> body) {
    Object token = body.get("confirm_token");
    var payload = token == null ? null : tokens.verify(String.valueOf(token));
    if (payload == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "INVALID_TOKEN"));
    }
    var r = orders.applyConfirmation(payload.negotiationId(), payload.side());
    if (r.error() != null) {
      return ResponseEntity.badRequest().body(Map.of("error", r.error()));
    }
    return ResponseEntity.ok(Map.of(
        "order_id", r.orderId(),
        "status", r.status(),
        "buyer_confirmed", r.buyerConfirmed(),
        "seller_confirmed", r.sellerConfirmed()));
  }

  /** Buy at list price, no negotiation. */
  @PostMapping("/api/orders/buy-now")
  public ResponseEntity<?> buyNow(@RequestBody Map<String, Object> body) {
    if (!CurrentUser.isBuyer()) return ResponseEntity.status(403).body(Map.of("error", "BUYER_ONLY"));
    int qty;
    try { qty = (int) Math.round(Double.parseDouble(String.valueOf(body.getOrDefault("quantity", 1)))); }
    catch (NumberFormatException e) { qty = 1; }
    var r = orders.buyNow(CurrentUser.id(), String.valueOf(body.get("product_id")), qty);
    if (r.error() != null) return ResponseEntity.badRequest().body(Map.of("error", r.error()));
    return ResponseEntity.ok(Map.of(
        "order_id", r.orderId(), "negotiation_id", r.negotiationId(), "total", r.total()));
  }

  /** The signed-in buyer's orders, newest first, with delivery tracking. */
  @GetMapping("/api/orders")
  public ResponseEntity<?> myOrders() {
    if (!CurrentUser.isBuyer()) return ResponseEntity.ok(List.of());
    return ResponseEntity.ok(orders.listOrders(CurrentUser.id()));
  }
}
