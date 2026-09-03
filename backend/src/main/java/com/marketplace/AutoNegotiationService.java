package com.marketplace;

import com.marketplace.db.*;
import com.marketplace.negotiation.*;
import org.springframework.stereotype.Service;

/**
 * Server-side "one call" negotiation for SELLER_MODE=server. Plays BOTH sides of
 * a haggle deterministically to a settled deal, so the buyer agent makes exactly
 * one tool call ({@code negotiate}) instead of driving a multi-round tool loop
 * that a small model routinely stalls partway through.
 *
 * <p>The seller side reuses the existing heuristic ({@link SellerResponderService});
 * the buyer side steps toward its ceiling. Every round is persisted through the
 * normal mutation path, so the chat transcript is a real, auditable haggle.
 */
@Service
public class AutoNegotiationService {

  private final NegotiationRepo negotiations;
  private final NegotiationRoundRepo rounds;
  private final NegotiationService negotiationService;
  private final SellerResponderService sellerResponder;

  public AutoNegotiationService(NegotiationRepo negotiations, NegotiationRoundRepo rounds,
      NegotiationService negotiationService, SellerResponderService sellerResponder) {
    this.negotiations = negotiations;
    this.rounds = rounds;
    this.negotiationService = negotiationService;
    this.sellerResponder = sellerResponder;
  }

  private static double round2(double n) {
    return Math.round(n * 100.0) / 100.0;
  }

  /**
   * Drive negotiation {@code negId} to a settled state. {@code ceiling} is the
   * highest total the buyer will accept. Returns the buyer's confirm token when
   * this method closed the deal by accepting the seller's price, or null when it
   * stopped for any other reason (seller accepted the buyer's price — the caller
   * then mints the buyer token; round cap hit above the ceiling; already
   * settled). The loop bound (2x the round cap) is a hard safety stop.
   */
  public String run(String negId, double ceiling) {
    for (int i = 0; i < OffersService.ROUND_CAP * 2 + 2; i++) {
      NegotiationEntity n = negotiations.findById(Long.valueOf(negId)).orElse(null);
      if (n == null) return null;
      if (!"open".equals(n.status) && !"countered".equals(n.status)) return null; // settled

      if ("buyer".equals(n.lastActor)) {
        sellerResponder.respond(negId); // seller's move: counter / accept / free-ship
        continue;
      }

      // The seller's price is on the table; it is the buyer's move.
      double sellerPrice = n.currentPrice;
      if (sellerPrice <= ceiling + 0.001) {
        var acc = commitBuyer(negId, TurnAction.ACCEPT, null, n.currentRound);
        sellerResponder.respond(negId); // confirm the seller side too
        return acc.ok() ? acc.confirmToken() : null;
      }
      if (n.currentRound >= OffersService.ROUND_CAP) return null; // no counters left, still too dear

      double lastBuyer = lastBuyerPrice(n.id, sellerPrice);
      double next = round2(Math.min(ceiling, lastBuyer + (sellerPrice - lastBuyer) / 2.0));
      if (next <= lastBuyer) next = round2(Math.min(ceiling, sellerPrice)); // no progress: jump
      if (!commitBuyer(negId, TurnAction.COUNTER, next, n.currentRound).ok()) return null;
    }
    return null;
  }

  private double lastBuyerPrice(Long negId, double fallback) {
    var rs = rounds.findByNegotiationIdOrderByRoundNumberAsc(negId);
    for (int i = rs.size() - 1; i >= 0; i--) {
      if ("buyer".equals(rs.get(i).author)) return rs.get(i).proposedPrice;
    }
    return fallback;
  }

  private NegotiationService.CommitResult commitBuyer(String negId, TurnAction a, Double price, int round) {
    return negotiationService.commitTurn(negId, new TurnInput(Side.BUYER, a, price, round, null));
  }
}
