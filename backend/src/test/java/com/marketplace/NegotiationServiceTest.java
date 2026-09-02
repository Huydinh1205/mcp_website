package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.db.*;
import com.marketplace.negotiation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** commitTurn -> applyTurn persistence half, against H2 (Long IDENTITY keys). */
@DataJpaTest
class NegotiationServiceTest {

  @Autowired NegotiationRepo negotiations;
  @Autowired NegotiationRoundRepo rounds;
  @Autowired ProductRepo products;
  @Autowired SellerConfigRepo sellerConfigs;
  @Autowired BuyerConfigRepo buyerConfigs;

  NegotiationService svc;
  Long productId;
  final Long buyerId = 501L;

  @BeforeEach
  void setup() {
    svc = new NegotiationService(
        negotiations, rounds, products, sellerConfigs, buyerConfigs,
        new OffersService(), new TokenService("test-secret"));

    ProductEntity p = new ProductEntity();
    p.name = "Keyboard"; p.price = 79; p.minPrice = 40; p.sellerId = 900L; p.remainings = 5;
    productId = products.save(p).id;

    SellerAiConfigEntity sc = new SellerAiConfigEntity();
    sc.productId = productId; sc.autoAcceptPrice = 55; sc.maxDiscountStep = 8;
    sellerConfigs.save(sc);

    BuyerAiConfigEntity bc = new BuyerAiConfigEntity();
    bc.buyerId = buyerId; bc.maxBudget = 60; bc.targetPrice = 45;
    buyerConfigs.save(bc);
  }

  String newNegotiation(String status, String lastActor, int round, double price) {
    NegotiationEntity n = new NegotiationEntity();
    n.status = status; n.lastActor = lastActor;
    n.currentRound = round; n.currentPrice = price;
    n.buyerId = buyerId; n.productId = productId;
    return String.valueOf(negotiations.save(n).id);
  }

  @Test
  void happyPath_offer_persistsRoundAndAdvancesStatus() {
    String id = newNegotiation("open", null, 1, 0);

    var res = svc.commitTurn(id, new TurnInput(Side.BUYER, TurnAction.OFFER, 45.0, 1, null));

    assertThat(res.ok()).isTrue();
    assertThat(res.status()).isEqualTo(NegotiationStatus.COUNTERED);
    assertThat(negotiations.findById(Long.valueOf(id)).orElseThrow().currentRound).isEqualTo(2);
    assertThat(rounds.findByNegotiationIdOrderByRoundNumberAsc(Long.valueOf(id))).hasSize(1);
  }

  @Test
  void notFound() {
    var res = svc.commitTurn("999999", new TurnInput(Side.BUYER, TurnAction.COUNTER, 48.0, 2, null));
    assertThat(res.ok()).isFalse();
    assertThat(res.error()).isEqualTo(TurnErrorCode.NOT_FOUND);
  }

  @Test
  void propagatesApplyTurnError_andWritesNothing() {
    String id = newNegotiation("countered", "buyer", 2, 45);

    var res = svc.commitTurn(id, new TurnInput(Side.BUYER, TurnAction.COUNTER, 48.0, 2, null));

    assertThat(res.ok()).isFalse();
    assertThat(res.error()).isEqualTo(TurnErrorCode.NOT_YOUR_TURN);
    assertThat(rounds.findByNegotiationIdOrderByRoundNumberAsc(Long.valueOf(id))).isEmpty();
  }

  @Test
  void staleRoundSeen() {
    String id = newNegotiation("countered", "seller", 2, 50);
    var res = svc.commitTurn(id, new TurnInput(Side.BUYER, TurnAction.COUNTER, 48.0, 1, null));
    assertThat(res.error()).isEqualTo(TurnErrorCode.STALE);
  }

  @Test
  void acceptMintsConfirmToken() {
    String id = newNegotiation("countered", "seller", 2, 52);
    var res = svc.commitTurn(id, new TurnInput(Side.BUYER, TurnAction.ACCEPT, null, 2, null));
    assertThat(res.ok()).isTrue();
    assertThat(res.status()).isEqualTo(NegotiationStatus.BUYER_ACCEPTED);
    assertThat(res.requiresHumanConfirmation()).isTrue();
    assertThat(res.confirmToken()).isNotBlank();
  }
}
