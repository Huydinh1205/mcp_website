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
 * A turn may carry multi-term deal content (price + quantity + freebies + free
 * shipping) via TurnInput.terms(). Only two hard economic limits are enforced —
 * buyer deal total <= budget, seller NET (price minus freebies given minus
 * shipping given) >= floor. Everything else (which freebie? B1G1 or not?) is
 * agent judgment, which is the point of using an agent.
 */
@Service
public class OffersService {

  // Shared-schema negotiations open at round 1 (CHK_Negotiation_CurrentRound),
  // so the opening offer lands on round 2. Cap of 8 leaves ~3 counter exchanges
  // before the seller is forced to settle — enough for a visible haggle.
  public static final int ROUND_CAP = 8;

  public TurnResult applyTurn(NegotiationSnapshot s, TurnInput in) {
    Side actor = in.actor();
    TurnAction action = in.action();
    DealTerms terms = in.terms();

    // 1. Frozen once finished.
    if (s.status() == CONFIRMED || s.status() == REJECTED) {
      return err(CLOSED);
    }

    // 1b. One side already accepted and it awaits human confirms. The ONLY move
    //     left is the OTHER side also accepting (so both get a confirm token);
    //     anything else — including re-accepting — is closed.
    if (s.status() == BUYER_ACCEPTED || s.status() == SELLER_ACCEPTED) {
      boolean buyerAlready = s.status() == BUYER_ACCEPTED;
      if (action == ACCEPT && actor == (buyerAlready ? SELLER : BUYER)) {
        return ok(new TurnOutcome(
            s.status(), actor, s.currentRound(), s.currentPrice(), false, true, null, null));
      }
      return err(CLOSED);
    }

    // 2. Optimistic round guard.
    //    - ACCEPT is always guarded: it mints a binding confirm token, and the
    //      caller has necessarily just read the state it is accepting, so it can
    //      always supply the round. A missing round (-1) fails here too.
    //    - OFFER / COUNTER are guarded only when the caller opts in by passing the
    //      round it acted on. Omitting it means "act on the current server state":
    //      moves are still bounded by budget / floor / step / round cap below, the
    //      DB write is a compare-and-set on the round, and the human confirms the
    //      final deal — so the worst case is a bounded counter the agent didn't
    //      pre-acknowledge, versus the agent stalling forever on a drifted round.
    boolean guardRound = action == ACCEPT || in.roundSeen() >= 0;
    if (guardRound && in.roundSeen() != s.currentRound()) {
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

    // 5. Economic bounds — before the round cap.
    //    priceOnTable = the deal total for this turn.
    double priceOnTable =
        action == ACCEPT
            ? s.currentPrice()
            : terms != null ? terms.price()
            : (in.price() == null ? Double.NaN : in.price());

    // What the seller gives away this turn (or, on accept, what is on the table).
    double freebiesGiven =
        action == ACCEPT ? s.currentFreebiesCost()
            : terms != null ? terms.freebiesCost()
            : 0.0;
    boolean shipGiven =
        action == ACCEPT ? s.currentFreeShipping()
            : terms != null && terms.freeShipping();
    double sellerNet = priceOnTable - freebiesGiven - (shipGiven ? s.shippingCost() : 0.0);

    if (actor == BUYER && priceOnTable > s.maxBudget()) {
      return err(OVER_BUDGET);
    }
    if (actor == SELLER && sellerNet < s.minPrice()) {
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

    // 7. Accept — locks the deal into a pending state; never places an order.
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

    // 8. offer / counter — advance one round, carrying the deal terms.
    String verb = action == OFFER ? "offers" : "counters at";
    String extra = terms == null ? "" : dealSuffix(terms);
    String message =
        in.message() != null
            ? in.message()
            : "%s %s %s%s".formatted(actor.wire(), verb, trimNum(priceOnTable), extra);
    return ok(new TurnOutcome(
        COUNTERED,
        actor,
        s.currentRound() + 1,
        priceOnTable,
        false,
        false,
        new TurnOutcome.AppendRound(actor, priceOnTable, message, terms),
        null));
  }

  private static String dealSuffix(DealTerms t) {
    StringBuilder b = new StringBuilder();
    if (t.quantity() > 1) b.append(" x").append(t.quantity());
    if (t.freebies() != null && !t.freebies().isEmpty()) {
      b.append(" + ").append(t.freebies().size()).append(" free item(s)");
    }
    if (t.freeShipping()) b.append(" + free shipping");
    return b.toString();
  }

  private static String trimNum(double n) {
    return n == Math.floor(n) ? String.valueOf((long) n) : String.valueOf(n);
  }
}
