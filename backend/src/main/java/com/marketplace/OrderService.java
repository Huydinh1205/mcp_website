package com.marketplace;

import com.marketplace.db.*;
import com.marketplace.negotiation.Side;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only path that finalizes an order (Constitution I). Two-sided confirm:
 * an [Order] row is created up front; when BOTH Buyer_Confirmed_At and
 * Seller_Confirmed_At are set, the order + negotiation flip to confirmed and an
 * [Invoice] row is written (which is what a [Feedback] review then hangs off).
 */
@Service
public class OrderService {

  private final NegotiationRepo negotiations;
  private final OrderRepo orders;
  private final InvoiceRepo invoices;
  private final DiscountService discounts;

  public OrderService(NegotiationRepo negotiations, OrderRepo orders, InvoiceRepo invoices,
      DiscountService discounts) {
    this.negotiations = negotiations;
    this.orders = orders;
    this.invoices = invoices;
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
    NegotiationEntity n = negotiations.findById(Long.valueOf(negotiationId)).orElse(null);
    if (n == null) return ConfirmResult.err("NOT_FOUND");
    if ("rejected".equals(n.status)) return ConfirmResult.err("NEGOTIATION_CLOSED");

    OrderEntity order;
    if (n.orderId == null) {
      order = new OrderEntity();
      order.status = "PENDING_PAYMENT";       // CHK_Order_Status
      orders.save(order);
      n.orderId = order.id;
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
    if (both && !"PAID".equals(order.status)) {
      order.status = "PAID";                  // CHK_Order_Status
      n.status = "confirmed";                 // CHK_Negotiation_Status (lowercase set)
      double coupon = discounts.totalDiscountFor(negotiationId, n.currentPrice);
      n.finalPrice = Math.round((n.currentPrice - coupon) * 100.0) / 100.0;
      negotiations.save(n);

      if (invoices.findByOrderId(order.id).isEmpty()) {
        InvoiceEntity inv = new InvoiceEntity();
        inv.orderId = order.id;
        inv.buyerId = n.buyerId;
        inv.paymentId = 1;                 // CreditCard
        inv.quantity = Math.max(1, n.quantity);
        inv.totalCost = n.finalPrice;
        invoices.save(inv);
      }
    }
    orders.save(order);

    return new ConfirmResult(
        String.valueOf(order.id),
        both ? "confirmed" : "pending",
        order.buyerConfirmedAt != null,
        order.sellerConfirmedAt != null,
        null);
  }
}
