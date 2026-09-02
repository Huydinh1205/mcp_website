package com.marketplace;

import com.marketplace.db.*;
import com.marketplace.negotiation.*;
import org.springframework.stereotype.Service;

/**
 * Server-side seller agent for US1 (app.seller-mode=server). Deterministic
 * heuristic (not an LLM) so the buyer flow always converges. Port of
 * lib/sellerResponder.ts. Runs after each buyer turn.
 */
@Service
public class SellerResponderService {

  private final NegotiationRepo negotiations;
  private final NegotiationRoundRepo rounds;
  private final ProductRepo products;
  private final SellerConfigRepo sellerConfigs;
  private final NegotiationService negotiationService;
  private final OrderService orderService;

  public SellerResponderService(NegotiationRepo negotiations, NegotiationRoundRepo rounds,
      ProductRepo products, SellerConfigRepo sellerConfigs,
      NegotiationService negotiationService, OrderService orderService) {
    this.negotiations = negotiations;
    this.rounds = rounds;
    this.products = products;
    this.sellerConfigs = sellerConfigs;
    this.negotiationService = negotiationService;
    this.orderService = orderService;
  }

  private static double round2(double n) {
    return Math.round(n * 100.0) / 100.0;
  }

  public void respond(String negotiationId) {
    NegotiationEntity n = negotiations.findById(Long.valueOf(negotiationId)).orElse(null);
    if (n == null) return;
    if (!"countered".equals(n.status) || !"buyer".equals(n.lastActor)) return;

    ProductEntity p = products.findById(n.productId).orElseThrow();
    var cfg = sellerConfigs.findByProductId(n.productId).orElse(null);
    double minPrice = p.minPrice;
    double autoAccept = cfg != null ? cfg.autoAcceptPrice : minPrice;
    double maxStep = cfg != null ? cfg.maxDiscountStep : p.price;
    double buyerPrice = n.currentPrice;

    double lastSeller = p.price;
    var rs = rounds.findByNegotiationIdOrderByRoundNumberAsc(n.id);
    for (int i = rs.size() - 1; i >= 0; i--) {
      if ("seller".equals(rs.get(i).author)) {
        lastSeller = rs.get(i).proposedPrice;
        break;
      }
    }

    if (buyerPrice >= autoAccept) {
      acceptAsSeller(negotiationId, n.currentRound);
      return;
    }

    double midpoint = buyerPrice + (lastSeller - buyerPrice) / 2.0;
    double counter = round2(Math.max(minPrice, Math.max(lastSeller - maxStep, midpoint)));

    if (counter <= buyerPrice + 0.5) {
      acceptAsSeller(negotiationId, n.currentRound);
      return;
    }

    // Close a near-deal by sweetening with FREE SHIPPING instead of another price cut.
    double gap = counter - buyerPrice;
    if (!n.currentFreeShipping && gap <= p.shippingCost + 1
        && buyerPrice - p.shippingCost >= minPrice) {
      negotiationService.commitTurn(negotiationId, new TurnInput(
          Side.SELLER, TurnAction.COUNTER, buyerPrice, n.currentRound,
          "I'll match your " + round2(buyerPrice) + " and throw in free shipping.",
          new com.marketplace.negotiation.DealTerms(buyerPrice, n.quantity, java.util.List.of(), 0, true)));
      return;
    }
    if (n.currentRound >= OffersService.ROUND_CAP && buyerPrice >= minPrice) {
      acceptAsSeller(negotiationId, n.currentRound);
      return;
    }

    negotiationService.commitTurn(negotiationId,
        new TurnInput(Side.SELLER, TurnAction.COUNTER, counter, n.currentRound,
            "How about " + counter + "?"));
  }

  private void acceptAsSeller(String negotiationId, int round) {
    var res = negotiationService.commitTurn(negotiationId,
        new TurnInput(Side.SELLER, TurnAction.ACCEPT, null, round, null));
    if (res.ok()) {
      // US1: seller pre-authorized -> record the seller side of the confirmation now.
      orderService.applyConfirmation(negotiationId, Side.SELLER);
    }
  }
}
