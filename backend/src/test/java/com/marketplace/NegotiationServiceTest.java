package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.db.*;
import com.marketplace.negotiation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Port of lib/negotiations.test.ts against H2. */
@DataJpaTest
class NegotiationServiceTest {

  @Autowired NegotiationRepo negotiations;
  @Autowired NegotiationRoundRepo rounds;
  @Autowired ProductRepo products;
  @Autowired SellerConfigRepo sellerConfigs;
  @Autowired BuyerConfigRepo buyerConfigs;

  NegotiationService svc;

  @BeforeEach
  void setup() {
    svc = new NegotiationService(
        negotiations, rounds, products, sellerConfigs, buyerConfigs,
        new OffersService(), new TokenService("test-secret"));

    ProductEntity p = new ProductEntity();
    p.productId = "P1"; p.name = "Keyboard"; p.price = 79; p.minPrice = 40;
    p.sellerId = "S1"; p.remainings = 5;
    products.save(p);

    SellerAiConfigEntity sc = new SellerAiConfigEntity();
    sc.agentId = "SC1"; sc.productId = "P1"; sc.autoAcceptPrice = 55; sc.maxDiscountStep = 8;
    sellerConfigs.save(sc);

    BuyerAiConfigEntity bc = new BuyerAiConfigEntity();
    bc.buyerAgentId = "BC1"; bc.nationalId = "B1"; bc.maxBudget = 60; bc.targetPrice = 45;
    buyerConfigs.save(bc);
  }

  NegotiationEntity newNegotiation(String id, String status, String lastActor, int round, double price) {
    NegotiationEntity n = new NegotiationEntity();
    n.negotiationId = id; n.status = status; n.lastActor = lastActor;
    n.currentRound = round; n.currentPrice = price;
    n.nationalId = "B1"; n.productId = "P1";
    return negotiations.save(n);
  }

  @Test
  void happyPath_offer_persistsRoundAndAdvancesStatus() {
    newNegotiation("N1", "open", null, 0, 0);

    var res = svc.commitTurn("N1", new TurnInput(Side.BUYER, TurnAction.OFFER, 45.0, 0, null));

    assertThat(res.ok()).isTrue();
    assertThat(res.status()).isEqualTo(NegotiationStatus.COUNTERED);
    assertThat(negotiations.findById("N1").orElseThrow().currentRound).isEqualTo(1);
    assertThat(rounds.findByNegotiationIdOrderByRoundNumberAsc("N1")).hasSize(1);
  }

  @Test
  void notFound() {
    var res = svc.commitTurn("missing", new TurnInput(Side.BUYER, TurnAction.COUNTER, 48.0, 2, null));
    assertThat(res.ok()).isFalse();
    assertThat(res.error()).isEqualTo(TurnErrorCode.NOT_FOUND);
  }

  @Test
  void propagatesApplyTurnError_andWritesNothing() {
    newNegotiation("N2", "countered", "buyer", 2, 45);

    var res = svc.commitTurn("N2", new TurnInput(Side.BUYER, TurnAction.COUNTER, 48.0, 2, null));

    assertThat(res.ok()).isFalse();
    assertThat(res.error()).isEqualTo(TurnErrorCode.NOT_YOUR_TURN);
    assertThat(rounds.findByNegotiationIdOrderByRoundNumberAsc("N2")).isEmpty();
  }

  @Test
  void staleRoundSeen() {
    newNegotiation("N3", "countered", "seller", 2, 50);
    var res = svc.commitTurn("N3", new TurnInput(Side.BUYER, TurnAction.COUNTER, 48.0, 1, null));
    assertThat(res.error()).isEqualTo(TurnErrorCode.STALE);
  }

  @Test
  void acceptMintsConfirmToken() {
    newNegotiation("N4", "countered", "seller", 2, 52);
    var res = svc.commitTurn("N4", new TurnInput(Side.BUYER, TurnAction.ACCEPT, null, 2, null));
    assertThat(res.ok()).isTrue();
    assertThat(res.status()).isEqualTo(NegotiationStatus.BUYER_ACCEPTED);
    assertThat(res.requiresHumanConfirmation()).isTrue();
    assertThat(res.confirmToken()).isNotBlank();
  }
}
