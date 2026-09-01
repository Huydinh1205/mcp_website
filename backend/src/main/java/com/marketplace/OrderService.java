package com.marketplace;

import com.marketplace.db.NegotiationEntity;
import com.marketplace.db.NegotiationRepo;
import com.marketplace.db.OrderEntity;
import com.marketplace.db.OrderRepo;
import com.marketplace.negotiation.Side;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only path that finalizes an order (Constitution I, FR-011, FR-012).
 * Port of applyConfirmation in lib/negotiationRepo.ts.
 */
@Service
public class OrderService {

  private final NegotiationRepo negotiations;
  private final OrderRepo orders;
  private final DiscountService discounts;

  public OrderService(NegotiationRepo negotiations, OrderRepo orders, DiscountService discounts) {
    this.negotiations = negotiations;
    this.orders = orders;
    this.discounts = discounts;
  }

  public record ConfirmResult(
      String orderId, String status, boolean buyerConfirmed, boolean sellerConfirmed, String error) {

    static ConfirmResult err(String e) {
      return new ConfirmResult(null, null, false, false, e);
    }
  }

  @Transactional
  public ConfirmResult applyConfirmation(String negotiationId, Side side) {
    NegotiationEntity n = negotiations.findById(negotiationId).orElse(null);
    if (n == null) return ConfirmResult.err("NOT_FOUND");
    if ("rejected".equals(n.status)) return ConfirmResult.err("NEGOTIATION_CLOSED");

    OrderEntity order;
    if (n.orderId == null) {
      order = new OrderEntity();
      order.orderId = UUID.randomUUID().toString().replace("-", "");
      order.status = "pending";
      orders.save(order);
      n.orderId = order.orderId;
      negotiations.save(n);
    } else {
      order = orders.findById(n.orderId).orElseThrow();
    }

    if (side == Side.BUYER && order.buyerConfirmedAt == null) {
      order.buyerConfirmedAt = Instant.now();
    } else if (side == Side.SELLER && order.sellerConfirmedAt == null) {
      order.sellerConfirmedAt = Instant.now();
    }

    boolean both = order.buyerConfirmedAt != null && order.sellerConfirmedAt != null;
    if (both && !"confirmed".equals(order.status)) {
      order.status = "confirmed";
      n.status = "confirmed";
      double coupon = discounts.totalDiscountFor(negotiationId, n.currentPrice);
      n.finalPrice = Math.round((n.currentPrice - coupon) * 100.0) / 100.0;
      negotiations.save(n);
    }
    orders.save(order);

    return new ConfirmResult(
        order.orderId,
        both ? "confirmed" : "pending",
        order.buyerConfirmedAt != null,
        order.sellerConfirmedAt != null,
        null);
  }
}
