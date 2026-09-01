package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.db.*;
import com.marketplace.negotiation.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Two-sided confirmation (D-A4, FR-011, FR-012). */
@DataJpaTest
class OrderServiceTest {

  @Autowired NegotiationRepo negotiations;
  @Autowired OrderRepo orders;
  @Autowired com.marketplace.db.DiscountRepo discountRepo;
  @Autowired com.marketplace.db.AppliedDiscountRepo appliedRepo;
  @Autowired com.marketplace.db.ProductRepo productRepo;

  OrderService svc;

  @BeforeEach
  void setup() {
    svc = new OrderService(negotiations, orders,
        new DiscountService(discountRepo, appliedRepo, productRepo));
  }

  NegotiationEntity seed(String id, String status, double price) {
    NegotiationEntity n = new NegotiationEntity();
    n.negotiationId = id;
    n.status = status;
    n.currentPrice = price;
    n.nationalId = "B1";
    n.productId = "P1";
    return negotiations.save(n);
  }

  @Test
  void firstBuyerConfirm_createsPendingOrder() {
    seed("N1", "buyer_accepted", 52);
    var r = svc.applyConfirmation("N1", Side.BUYER);
    assertThat(r.error()).isNull();
    assertThat(r.status()).isEqualTo("pending");
    assertThat(r.buyerConfirmed()).isTrue();
    assertThat(r.sellerConfirmed()).isFalse();
    assertThat(negotiations.findById("N1").orElseThrow().status).isEqualTo("buyer_accepted");
  }

  @Test
  void bothConfirm_finalizesOrderAndNegotiation() {
    seed("N2", "seller_accepted", 48);
    svc.applyConfirmation("N2", Side.BUYER);
    var r = svc.applyConfirmation("N2", Side.SELLER);
    assertThat(r.status()).isEqualTo("confirmed");
    assertThat(r.buyerConfirmed()).isTrue();
    assertThat(r.sellerConfirmed()).isTrue();
    NegotiationEntity n = negotiations.findById("N2").orElseThrow();
    assertThat(n.status).isEqualTo("confirmed");
    assertThat(n.finalPrice).isEqualTo(48.0);
    assertThat(orders.count()).isEqualTo(1);
  }

  @Test
  void repeatSameSide_isIdempotent() {
    seed("N3", "buyer_accepted", 50);
    svc.applyConfirmation("N3", Side.BUYER);
    var r = svc.applyConfirmation("N3", Side.BUYER);
    assertThat(r.status()).isEqualTo("pending");
    assertThat(orders.count()).isEqualTo(1);
  }

  @Test
  void rejectedNegotiation_isClosed() {
    seed("N4", "rejected", 0);
    var r = svc.applyConfirmation("N4", Side.BUYER);
    assertThat(r.error()).isEqualTo("NEGOTIATION_CLOSED");
  }

  @Test
  void missingNegotiation_notFound() {
    var r = svc.applyConfirmation("nope", Side.BUYER);
    assertThat(r.error()).isEqualTo("NOT_FOUND");
  }
}
