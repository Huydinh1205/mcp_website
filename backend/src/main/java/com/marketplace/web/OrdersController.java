package com.marketplace.web;

import com.marketplace.OrderService;
import com.marketplace.TokenService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
