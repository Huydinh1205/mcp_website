package com.marketplace;

import com.marketplace.db.*;
import com.marketplace.negotiation.Side;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finalizes orders (Constitution I).
 *  - applyConfirmation(): two-sided human confirm of a negotiated deal.
 *  - buyNow(): skip negotiation, buy at list price straight away.
 * Both produce [Order] + [Invoice]; buyNow also creates a [Delivery] row so
 * the order can be tracked.
 */
@Service
public class OrderService {

  private final NegotiationRepo negotiations;
  private final OrderRepo orders;
  private final InvoiceRepo invoices;
  private final DeliveryRepo deliveries;
  private final ProductRepo products;
  private final UserRepo users;
  private final FeedbackRepo feedback;
  private final DiscountService discounts;

  public OrderService(NegotiationRepo negotiations, OrderRepo orders, InvoiceRepo invoices,
      DeliveryRepo deliveries, ProductRepo products, UserRepo users, FeedbackRepo feedback,
      DiscountService discounts) {
    this.negotiations = negotiations;
    this.orders = orders;
    this.invoices = invoices;
    this.deliveries = deliveries;
    this.products = products;
    this.users = users;
    this.feedback = feedback;
    this.discounts = discounts;
  }

  private static double round2(double n) { return Math.round(n * 100.0) / 100.0; }

  private static String imageOf(ProductEntity p) {
    return p.imageUrl != null && !p.imageUrl.isBlank()
        ? p.imageUrl : "https://picsum.photos/seed/prod" + p.id + "/500/600";
  }

  private String addressOf(Long buyerId) {
    return users.findById(buyerId)
        .map(u -> (u.firstName + " " + u.lastName).trim() + " — address on file")
        .orElse("Address on file");
  }

  // ------------------------------------------------------------------ buy now

  public record BuyResult(String orderId, String negotiationId, double total, String error) {
    static BuyResult err(String e) { return new BuyResult(null, null, 0, e); }
  }

  @Transactional
  public BuyResult buyNow(String buyerId, String productId, int quantity) {
    ProductEntity p = products.findById(Long.valueOf(productId)).orElse(null);
    if (p == null) return BuyResult.err("PRODUCT_NOT_FOUND");
    int qty = Math.max(1, quantity);
    double total = round2(p.price * qty);

    NegotiationEntity n = new NegotiationEntity();
    n.buyerId = Long.valueOf(buyerId);
    n.productId = p.id;
    n.quantity = qty;
    n.currentRound = 1;
    n.currentPrice = p.price;
    n.status = "confirmed";
    n.finalPrice = total;

    DeliveryEntity d = new DeliveryEntity();
    d.trackingNumber = "TRK" + Long.toString(System.nanoTime(), 36).toUpperCase();
    d.address = addressOf(n.buyerId);
    d.status = "PENDING";
    d.estimatedDate = Instant.now().plus(5, ChronoUnit.DAYS);
    deliveries.save(d);

    OrderEntity o = new OrderEntity();
    o.status = "PAID";
    o.deliveryId = d.id;
    o.buyerConfirmedAt = Instant.now();
    o.sellerConfirmedAt = Instant.now();
    orders.save(o);

    n.orderId = o.id;
    negotiations.save(n);

    InvoiceEntity inv = new InvoiceEntity();
    inv.orderId = o.id;
    inv.buyerId = n.buyerId;
    inv.paymentId = 1;
    inv.quantity = qty;
    inv.totalCost = total;
    invoices.save(inv);

    return new BuyResult(String.valueOf(o.id), String.valueOf(n.id), total, null);
  }

  // --------------------------------------------------------------- my orders

  public List<Map<String, Object>> listOrders(String buyerId) {
    return negotiations.findByBuyerId(Long.valueOf(buyerId)).stream()
        .filter(n -> "confirmed".equals(n.status) && n.orderId != null)
        .map(n -> {
          OrderEntity o = orders.findById(n.orderId).orElse(null);
          ProductEntity p = products.findById(n.productId).orElse(null);
          DeliveryEntity d = o != null && o.deliveryId != null
              ? deliveries.findById(o.deliveryId).orElse(null) : null;
          boolean reviewed = invoices.findByOrderId(n.orderId).stream().findFirst()
              .map(iv -> !feedback.findByInvoiceId(iv.id).isEmpty()).orElse(false);
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("order_id", String.valueOf(n.orderId));
          m.put("negotiation_id", String.valueOf(n.id));
          m.put("product_id", p == null ? null : String.valueOf(p.id));
          m.put("name", p == null ? "?" : p.name);
          m.put("image_url", p == null ? null : imageOf(p));
          m.put("quantity", n.quantity);
          m.put("unit_price", n.currentPrice);
          m.put("total", n.finalPrice != null ? n.finalPrice : n.currentPrice * n.quantity);
          m.put("negotiated", n.currentRound > 1);
          m.put("order_status", o == null ? "PAID" : o.status);
          m.put("delivery_status", d == null ? "PENDING" : d.status);
          m.put("tracking_number", d == null ? null : d.trackingNumber);
          m.put("estimated_date", d == null ? null : d.estimatedDate);
          m.put("ordered_at", o == null ? null : o.orderDate);
          m.put("reviewed", reviewed);
          return m;
        })
        .sorted((a, b) -> String.valueOf(b.get("ordered_at")).compareTo(String.valueOf(a.get("ordered_at"))))
        .toList();
  }

  // ------------------------------------------------------ two-sided confirm

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
      order.status = "PENDING_PAYMENT";
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
      order.status = "PAID";
      n.status = "confirmed";
      double coupon = discounts.totalDiscountFor(negotiationId, n.currentPrice);
      n.finalPrice = round2(n.currentPrice - coupon);
      negotiations.save(n);

      if (order.deliveryId == null) {
        DeliveryEntity d = new DeliveryEntity();
        d.trackingNumber = "TRK" + Long.toString(System.nanoTime(), 36).toUpperCase();
        d.address = addressOf(n.buyerId);
        d.status = "PENDING";
        d.estimatedDate = Instant.now().plus(5, ChronoUnit.DAYS);
        deliveries.save(d);
        order.deliveryId = d.id;
      }
      if (invoices.findByOrderId(order.id).isEmpty()) {
        InvoiceEntity inv = new InvoiceEntity();
        inv.orderId = order.id;
        inv.buyerId = n.buyerId;
        inv.paymentId = 1;
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
