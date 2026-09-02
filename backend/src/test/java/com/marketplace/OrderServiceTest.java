package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.db.*;
import com.marketplace.negotiation.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Two-sided confirmation -> [Order] + [Invoice], against H2. */
@DataJpaTest
class OrderServiceTest {

  @Autowired NegotiationRepo negotiations;
  @Autowired OrderRepo orders;
  @Autowired InvoiceRepo invoices;
  @Autowired DiscountRepo discountRepo;
  @Autowired AppliedDiscountRepo appliedRepo;
  @Autowired ProductRepo productRepo;

  OrderService svc;

  @BeforeEach
  void setup() {
    svc = new OrderService(negotiations, orders, invoices,
        new DiscountService(discountRepo, appliedRepo, productRepo));
  }

  String seed(String status, double price) {
    NegotiationEntity n = new NegotiationEntity();
    n.status = status;
    n.currentPrice = price;
    n.buyerId = 501L;
    n.productId = 900L;
    return String.valueOf(negotiations.save(n).id);
  }

  @Test
  void firstBuyerConfirm_createsPendingOrder() {
    String id = seed("buyer_accepted", 52);
    var r = svc.applyConfirmation(id, Side.BUYER);
    assertThat(r.error()).isNull();
    assertThat(r.status()).isEqualTo("pending");
    assertThat(r.buyerConfirmed()).isTrue();
    assertThat(r.sellerConfirmed()).isFalse();
    assertThat(negotiations.findById(Long.valueOf(id)).orElseThrow().status).isEqualTo("buyer_accepted");
  }

  @Test
  void bothConfirm_finalizesOrderAndWritesInvoice() {
    String id = seed("seller_accepted", 48);
    svc.applyConfirmation(id, Side.BUYER);
    var r = svc.applyConfirmation(id, Side.SELLER);
    assertThat(r.status()).isEqualTo("confirmed");
    assertThat(r.buyerConfirmed()).isTrue();
    assertThat(r.sellerConfirmed()).isTrue();
    NegotiationEntity n = negotiations.findById(Long.valueOf(id)).orElseThrow();
    assertThat(n.status).isEqualTo("confirmed");
    assertThat(n.finalPrice).isEqualTo(48.0);
    assertThat(orders.count()).isEqualTo(1);
    assertThat(invoices.findByOrderId(n.orderId)).hasSize(1);
    assertThat(invoices.findByOrderId(n.orderId).get(0).totalCost).isEqualTo(48.0);
  }

  @Test
  void repeatSameSide_isIdempotent() {
    String id = seed("buyer_accepted", 50);
    svc.applyConfirmation(id, Side.BUYER);
    var r = svc.applyConfirmation(id, Side.BUYER);
    assertThat(r.status()).isEqualTo("pending");
    assertThat(orders.count()).isEqualTo(1);
  }

  @Test
  void rejectedNegotiation_isClosed() {
    String id = seed("rejected", 0);
    var r = svc.applyConfirmation(id, Side.BUYER);
    assertThat(r.error()).isEqualTo("NEGOTIATION_CLOSED");
  }

  @Test
  void missingNegotiation_notFound() {
    var r = svc.applyConfirmation("999999", Side.BUYER);
    assertThat(r.error()).isEqualTo("NOT_FOUND");
  }
}
