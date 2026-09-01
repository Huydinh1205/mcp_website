package com.marketplace.negotiation;

import static com.marketplace.negotiation.NegotiationStatus.*;
import static com.marketplace.negotiation.Side.*;
import static com.marketplace.negotiation.TurnAction.*;
import static com.marketplace.negotiation.TurnErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import java.util.List;

/** Port of lib/offers.test.ts — the negotiation state machine. */
class OffersServiceTest {

  private final OffersService offers = new OffersService();

  /** Mutable builder so each test overrides only what it exercises. */
  static final class Snap {
    NegotiationStatus status = COUNTERED;
    Side lastActor = SELLER;
    int currentRound = 2;
    double currentPrice = 50;
    double maxBudget = 60;
    double minPrice = 40;
    double maxDiscountStep = 10;
    Double lastSellerPrice = 55.0;
    double shippingCost = 5;
    double currentFreebiesCost = 0;
    boolean currentFreeShipping = false;

    NegotiationSnapshot build() {
      return new NegotiationSnapshot(
          status, lastActor, currentRound, currentPrice,
          maxBudget, minPrice, maxDiscountStep, lastSellerPrice,
          shippingCost, currentFreebiesCost, currentFreeShipping);
    }
  }

  static Snap snap() {
    return new Snap();
  }

  static TurnOutcome okValue(TurnResult r) {
    assertThat(r).isInstanceOf(TurnResult.Ok.class);
    return ((TurnResult.Ok) r).value();
  }

  @Test
  void happyPath_offer_counter_accept() {
    Snap open = snap();
    open.status = OPEN;
    open.lastActor = null;
    open.currentRound = 0;
    open.currentPrice = 0;
    open.lastSellerPrice = null;

    TurnOutcome offer =
        okValue(offers.applyTurn(open.build(), new TurnInput(BUYER, OFFER, 45.0, 0, null)));
    assertThat(offer.nextStatus()).isEqualTo(COUNTERED);
    assertThat(offer.nextLastActor()).isEqualTo(BUYER);
    assertThat(offer.nextRound()).isEqualTo(1);
    assertThat(offer.nextPrice()).isEqualTo(45);
    assertThat(offer.appendRound()).isNotNull();
    assertThat(offer.appendRound().author()).isEqualTo(BUYER);
    assertThat(offer.appendRound().proposedPrice()).isEqualTo(45);

    Snap afterOffer = snap();
    afterOffer.lastActor = BUYER;
    afterOffer.currentRound = 1;
    afterOffer.currentPrice = 45;
    afterOffer.lastSellerPrice = null;
    TurnOutcome counter =
        okValue(offers.applyTurn(afterOffer.build(), new TurnInput(SELLER, COUNTER, 52.0, 1, null)));
    assertThat(counter.nextStatus()).isEqualTo(COUNTERED);
    assertThat(counter.nextLastActor()).isEqualTo(SELLER);
    assertThat(counter.nextRound()).isEqualTo(2);
    assertThat(counter.nextPrice()).isEqualTo(52);

    Snap afterCounter = snap();
    afterCounter.lastActor = SELLER;
    afterCounter.currentRound = 2;
    afterCounter.currentPrice = 52;
    TurnOutcome accept =
        okValue(offers.applyTurn(afterCounter.build(), new TurnInput(BUYER, ACCEPT, null, 2, null)));
    assertThat(accept.nextStatus()).isEqualTo(BUYER_ACCEPTED);
    assertThat(accept.requiresHumanConfirmation()).isTrue();
    assertThat(accept.nextPrice()).isEqualTo(52);
    assertThat(accept.appendRound()).isNull();
  }

  @Test
  void staleTurn() {
    TurnResult r =
        offers.applyTurn(snap().build(), new TurnInput(BUYER, COUNTER, 48.0, 1, null));
    assertThat(r).isEqualTo(new TurnResult.Err(STALE));
  }

  @Test
  void wrongActor() {
    Snap s = snap();
    s.lastActor = BUYER;
    TurnResult r = offers.applyTurn(s.build(), new TurnInput(BUYER, COUNTER, 48.0, 2, null));
    assertThat(r).isEqualTo(new TurnResult.Err(NOT_YOUR_TURN));
  }

  @Test
  void buyerOverBudget() {
    Snap s = snap();
    s.status = OPEN;
    s.lastActor = null;
    s.currentRound = 0;
    s.lastSellerPrice = null;
    TurnResult r = offers.applyTurn(s.build(), new TurnInput(BUYER, OFFER, 100.0, 0, null));
    assertThat(r).isEqualTo(new TurnResult.Err(OVER_BUDGET));
  }

  @Test
  void sellerBelowFloor() {
    Snap s = snap();
    s.lastActor = BUYER;
    s.currentRound = 3;
    TurnResult r = offers.applyTurn(s.build(), new TurnInput(SELLER, COUNTER, 30.0, 3, null));
    assertThat(r).isEqualTo(new TurnResult.Err(BELOW_FLOOR));
  }

  @Test
  void roundCapReached() {
    Snap s = snap();
    s.lastActor = SELLER;
    s.currentRound = OffersService.ROUND_CAP;
    TurnOutcome o =
        okValue(offers.applyTurn(s.build(), new TurnInput(BUYER, COUNTER, 47.0, OffersService.ROUND_CAP, null)));
    assertThat(o.nextStatus()).isEqualTo(REJECTED);
    assertThat(o.terminal()).isTrue();
    assertThat(o.reason()).isEqualTo("round_cap");
  }

  @Test
  void terminalNegotiationClosed() {
    Snap s = snap();
    s.status = CONFIRMED;
    TurnResult r = offers.applyTurn(s.build(), new TurnInput(BUYER, ACCEPT, null, 2, null));
    assertThat(r).isEqualTo(new TurnResult.Err(CLOSED));
  }

  @Test
  void acceptedStateFrozen() {
    for (NegotiationStatus st : new NegotiationStatus[] {BUYER_ACCEPTED, SELLER_ACCEPTED}) {
      Snap s = snap();
      s.status = st;
      TurnResult r = offers.applyTurn(s.build(), new TurnInput(SELLER, COUNTER, 50.0, 2, null));
      assertThat(r).isEqualTo(new TurnResult.Err(CLOSED));
    }
  }

  @Test
  void acceptAddsNoTranscriptRound() {
    Snap s = snap();
    s.lastActor = SELLER;
    s.currentRound = 2;
    s.currentPrice = 52;
    TurnOutcome o = okValue(offers.applyTurn(s.build(), new TurnInput(BUYER, ACCEPT, null, 2, null)));
    assertThat(o.appendRound()).isNull();
  }

  @Test
  void sellerRejectEndsNegotiation() {
    Snap s = snap();
    s.lastActor = BUYER;
    TurnOutcome o = okValue(offers.applyTurn(s.build(), new TurnInput(SELLER, REJECT, null, 2, null)));
    assertThat(o.nextStatus()).isEqualTo(REJECTED);
    assertThat(o.terminal()).isTrue();
    assertThat(o.appendRound()).isNull();
  }

  @Test
  void sellerCounterExceedingStep() {
    Snap s = snap();
    s.lastActor = BUYER;
    s.lastSellerPrice = 55.0;
    s.maxDiscountStep = 8;
    TurnResult r = offers.applyTurn(s.build(), new TurnInput(SELLER, COUNTER, 44.0, 2, null));
    assertThat(r).isEqualTo(new TurnResult.Err(STEP_TOO_BIG));
  }

  @Test
  void sellerCounterWithinStepAllowed() {
    Snap s = snap();
    s.lastActor = BUYER;
    s.lastSellerPrice = 55.0;
    s.maxDiscountStep = 8;
    TurnResult r = offers.applyTurn(s.build(), new TurnInput(SELLER, COUNTER, 48.0, 2, null));
    assertThat(r).isInstanceOf(TurnResult.Ok.class);
  }

  // ---- multi-term deals (price + quantity + freebies + free shipping) --------

  static TurnInput sellerCounterTerms(double price, double freebiesCost, boolean freeShip, int roundSeen) {
    return new TurnInput(SELLER, COUNTER, price, roundSeen, null,
        new DealTerms(price, 1, List.of("addon"), freebiesCost, freeShip));
  }

  @Test
  void sellerFreebieThatDropsNetBelowFloor_isRejected() {
    Snap s = snap();
    s.lastActor = BUYER;
    s.minPrice = 40;
    // price 45, give away a freebie worth 10 -> net 35 < 40
    TurnResult r = offers.applyTurn(s.build(), sellerCounterTerms(45, 10, false, 2));
    assertThat(r).isEqualTo(new TurnResult.Err(BELOW_FLOOR));
  }

  @Test
  void sellerFreebieWhereNetStillClearsFloor_isAllowed_andCarriesTerms() {
    Snap s = snap();
    s.lastActor = BUYER;
    s.minPrice = 40;
    // price 55, freebie worth 10 -> net 45 >= 40
    TurnOutcome o = okValue(offers.applyTurn(s.build(), sellerCounterTerms(55, 10, false, 2)));
    assertThat(o.nextStatus()).isEqualTo(COUNTERED);
    assertThat(o.appendRound()).isNotNull();
    assertThat(o.appendRound().terms()).isNotNull();
    assertThat(o.appendRound().terms().freebiesCost()).isEqualTo(10.0);
  }

  @Test
  void freeShippingCountsAgainstSellerNet() {
    Snap s = snap();
    s.lastActor = BUYER;
    s.minPrice = 40;
    s.shippingCost = 5;
    // price 42, free shipping -> net 37 < 40
    TurnResult r = offers.applyTurn(s.build(), sellerCounterTerms(42, 0, true, 2));
    assertThat(r).isEqualTo(new TurnResult.Err(BELOW_FLOOR));
  }

  @Test
  void buyerDealTotalCheckedAgainstBudget() {
    Snap s = snap();
    s.status = OPEN;
    s.lastActor = null;
    s.currentRound = 0;
    s.lastSellerPrice = null;
    s.maxBudget = 60;
    TurnInput offer = new TurnInput(BUYER, OFFER, 65.0, 0, null,
        new DealTerms(65, 2, List.of(), 0, false)); // deal total 65 for 2 units
    assertThat(offers.applyTurn(s.build(), offer)).isEqualTo(new TurnResult.Err(OVER_BUDGET));
  }

  @Test
  void sellerAcceptRechecksItsOwnNetWithCurrentFreebies() {
    Snap s = snap();
    s.lastActor = BUYER;
    s.currentRound = 2;
    s.currentPrice = 45;
    s.minPrice = 40;
    s.currentFreebiesCost = 10; // an earlier round already put a freebie on the table
    TurnResult r = offers.applyTurn(s.build(), new TurnInput(SELLER, ACCEPT, null, 2, null));
    assertThat(r).isEqualTo(new TurnResult.Err(BELOW_FLOOR));
  }
}
