package com.marketplace.negotiation;

import static com.marketplace.negotiation.NegotiationStatus.*;
import static com.marketplace.negotiation.Side.*;
import static com.marketplace.negotiation.TurnAction.*;
import static com.marketplace.negotiation.TurnErrorCode.*;
import static com.marketplace.negotiation.TurnResult.err;
import static com.marketplace.negotiation.TurnResult.ok;

import org.springframework.stereotype.Service;

/**
 * The ONLY code permitted to decide a negotiation state transition
 * (Constitution IV). Pure: takes a snapshot, returns the intended outcome or a
 * typed error. Persistence + the optimistic write live in NegotiationService.
 *
 * Port of lib/offers.ts. Rule ordering mirrors data-model.md "Transition rules".
 */
@Service
public class OffersService {

  public static final int ROUND_CAP = 3;

  public TurnResult applyTurn(NegotiationSnapshot s, TurnInput in) {
    Side actor = in.actor();
    TurnAction action = in.action();

    // 1. Frozen once finished, or once a side accepted and it awaits human confirms.
    if (s.status() == CONFIRMED
        || s.status() == REJECTED
        || s.status() == BUYER_ACCEPTED
        || s.status() == SELLER_ACCEPTED) {
      return err(CLOSED);
    }

    // 2. Optimistic round guard.
    if (in.roundSeen() != s.currentRound()) {
      return err(STALE);
    }

    // 3. Actor gate. "offer" opens a negotiation; everything else alternates.
    if (action == OFFER) {
      if (s.status() != OPEN) return err(NOT_YOUR_TURN);
    } else if (s.lastActor() == actor) {
      return err(NOT_YOUR_TURN);
    }

    // 4. Seller walks away.
    if (action == REJECT) {
      return ok(new TurnOutcome(
          REJECTED, actor, s.currentRound(), s.currentPrice(), true, false, null, null));
    }

    // 5. Price bounds — before the round cap (buyer ceiling / seller floor first).
    double priceOnTable =
        action == ACCEPT ? s.currentPrice() : (in.price() == null ? Double.NaN : in.price());

    if (actor == BUYER && priceOnTable > s.maxBudget()) {
      return err(OVER_BUDGET);
    }
    if (actor == SELLER && priceOnTable < s.minPrice()) {
      return err(BELOW_FLOOR);
    }
    if (actor == SELLER
        && action == COUNTER
        && s.lastSellerPrice() != null
        && s.lastSellerPrice() - priceOnTable > s.maxDiscountStep()) {
      return err(STEP_TOO_BIG);
    }

    // 6. Round cap: a valid counter past the cap ends the negotiation, no deal.
    if (action == COUNTER && s.currentRound() >= ROUND_CAP) {
      return ok(new TurnOutcome(
          REJECTED, actor, s.currentRound(), s.currentPrice(), true, false, null, "round_cap"));
    }

    // 7. Accept — locks the price into a pending deal; never places an order.
    if (action == ACCEPT) {
      return ok(new TurnOutcome(
          actor == BUYER ? BUYER_ACCEPTED : SELLER_ACCEPTED,
          actor,
          s.currentRound(),
          s.currentPrice(),
          false,
          true,
          null,
          null));
    }

    // 8. offer / counter — advance one round.
    String verb = action == OFFER ? "offers" : "counters at";
    String message =
        in.message() != null
            ? in.message()
            : "%s %s %s".formatted(actor.wire(), verb, trimNum(priceOnTable));
    return ok(new TurnOutcome(
        COUNTERED,
        actor,
        s.currentRound() + 1,
        priceOnTable,
        false,
        false,
        new TurnOutcome.AppendRound(actor, priceOnTable, message),
        null));
  }

  private static String trimNum(double n) {
    return n == Math.floor(n) ? String.valueOf((long) n) : String.valueOf(n);
  }
}
