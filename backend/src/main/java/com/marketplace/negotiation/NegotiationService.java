package com.marketplace.negotiation;

import com.marketplace.FeedService;
import com.marketplace.TokenService;
import com.marketplace.db.*;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence half of the single mutation path. Port of lib/negotiations.ts +
 * the parts of lib/negotiationRepo.ts the flow uses. Every negotiation state
 * change goes through commitTurn -> OffersService.applyTurn (Constitution IV).
 */
@Service
public class NegotiationService {

  private static final List<String> LIVE =
      List.of("open", "countered", "buyer_accepted", "seller_accepted");

  private final NegotiationRepo negotiations;
  private final NegotiationRoundRepo rounds;
  private final ProductRepo products;
  private final SellerConfigRepo sellerConfigs;
  private final BuyerConfigRepo buyerConfigs;
  private final OffersService offers;
  private final TokenService tokens;
  private final ObjectMapper json = new ObjectMapper();

  public NegotiationService(
      NegotiationRepo negotiations,
      NegotiationRoundRepo rounds,
      ProductRepo products,
      SellerConfigRepo sellerConfigs,
      BuyerConfigRepo buyerConfigs,
      OffersService offers,
      TokenService tokens) {
    this.negotiations = negotiations;
    this.rounds = rounds;
    this.products = products;
    this.sellerConfigs = sellerConfigs;
    this.buyerConfigs = buyerConfigs;
    this.offers = offers;
    this.tokens = tokens;
  }

  public record CommitResult(
      boolean ok,
      NegotiationStatus status,
      boolean requiresHumanConfirmation,
      String confirmToken,
      TurnErrorCode error) {

    static CommitResult ok(NegotiationStatus s, boolean rhc, String tok) {
      return new CommitResult(true, s, rhc, tok, null);
    }

    static CommitResult err(TurnErrorCode e) {
      return new CommitResult(false, null, false, null, e);
    }
  }

  public NegotiationSnapshot loadSnapshot(String id) {
    Optional<NegotiationEntity> nOpt = negotiations.findById(id);
    if (nOpt.isEmpty()) return null;
    NegotiationEntity n = nOpt.get();

    ProductEntity product = products.findById(n.productId).orElseThrow();
    var sConf = sellerConfigs.findByProductId(n.productId).orElse(null);
    var bConf = buyerConfigs.findByNationalId(n.nationalId).orElse(null);

    var rs = rounds.findByNegotiationIdOrderByRoundNumberAsc(id);
    Double lastSeller = null;
    for (int i = rs.size() - 1; i >= 0; i--) {
      if ("seller".equals(rs.get(i).author)) {
        lastSeller = rs.get(i).proposedPrice;
        break;
      }
    }

    return new NegotiationSnapshot(
        NegotiationStatus.fromWire(n.status),
        n.lastActor == null ? null : Side.fromWire(n.lastActor),
        n.currentRound,
        n.currentPrice,
        bConf != null ? bConf.maxBudget : Double.POSITIVE_INFINITY,
        product.minPrice,
        sConf != null ? sConf.maxDiscountStep : Double.POSITIVE_INFINITY,
        lastSeller,
        product.shippingCost,
        n.currentFreebiesCost,
        n.currentFreeShipping);
  }

  @Transactional
  public CommitResult commitTurn(String negotiationId, TurnInput input) {
    NegotiationSnapshot snap = loadSnapshot(negotiationId);
    if (snap == null) return CommitResult.err(TurnErrorCode.NOT_FOUND);

    TurnResult r = offers.applyTurn(snap, input);
    if (r instanceof TurnResult.Err e) return CommitResult.err(e.error());
    TurnOutcome o = ((TurnResult.Ok) r).value();

    com.marketplace.negotiation.DealTerms t =
        o.appendRound() != null ? o.appendRound().terms() : null;
    double freebiesCost = t != null ? t.freebiesCost() : 0.0;
    boolean freeShip = t != null && t.freeShipping();
    int qty = t != null && t.quantity() > 0 ? t.quantity() : 1;

    int written = negotiations.applyTurn(
        negotiationId,
        java.time.Instant.now(),
        snap.currentRound(),
        o.nextStatus().wire(),
        o.nextLastActor().wire(),
        o.nextRound(),
        o.nextPrice(),
        freebiesCost,
        freeShip,
        qty);
    if (written == 0) return CommitResult.err(TurnErrorCode.STALE);

    if (o.appendRound() != null) {
      NegotiationRoundEntity nr = new NegotiationRoundEntity();
      nr.negotiationId = negotiationId;
      nr.roundNumber = o.nextRound();
      nr.proposedPrice = o.appendRound().proposedPrice();
      nr.messageContext = o.appendRound().message();
      nr.author = o.appendRound().author().wire();
      if (t != null) {
        try { nr.terms = json.writeValueAsString(t); } catch (Exception ignored) {}
      }
      rounds.save(nr);
    }

    String token =
        o.requiresHumanConfirmation() ? tokens.mint(negotiationId, input.actor()) : null;
    return CommitResult.ok(o.nextStatus(), o.requiresHumanConfirmation(), token);
  }

  /** Ownership guards — IDOR protection for negotiation_id inputs. */
  public boolean buyerOwns(String negotiationId, String buyerId) {
    return negotiations.findById(negotiationId)
        .map(n -> buyerId != null && buyerId.equals(n.nationalId)).orElse(false);
  }

  public boolean sellerOwns(String negotiationId, String sellerId) {
    var n = negotiations.findById(negotiationId).orElse(null);
    if (n == null || sellerId == null) return false;
    return products.findById(n.productId).map(p -> sellerId.equals(p.sellerId)).orElse(false);
  }

  // --- reads for the WebMCP tools / feed -------------------------------------

  public NegotiationEntity create(String buyerId, String productId, int quantity) {
    NegotiationEntity n = new NegotiationEntity();
    n.negotiationId = java.util.UUID.randomUUID().toString().replace("-", "");
    n.nationalId = buyerId;
    n.productId = productId;
    n.quantity = Math.max(1, quantity);
    n.status = "open";
    n.currentRound = 0;
    return negotiations.save(n);
  }

  public Optional<NegotiationEntity> findOpenForBuyer(String buyerId, String productId) {
    return negotiations.findByNationalIdAndProductIdAndStatusIn(buyerId, productId, LIVE)
        .stream().findFirst();
  }

  public List<FeedService.FeedRow> feedRows(String buyerId, String sellerId) {
    List<NegotiationEntity> ns;
    if (buyerId != null) {
      ns = negotiations.findByNationalId(buyerId);
    } else if (sellerId != null) {
      var productIds = products.findBySellerId(sellerId).stream().map(p -> p.productId).toList();
      ns = productIds.isEmpty() ? List.of()
          : negotiations.findByProductIdInAndStatusIn(productIds,
              List.of("open", "countered", "buyer_accepted", "seller_accepted", "confirmed", "rejected"));
    } else {
      ns = negotiations.findAll();
    }
    return ns.stream().map(n -> {
      var rs = rounds.findByNegotiationIdOrderByRoundNumberAsc(n.negotiationId).stream()
          .map(r -> new FeedService.FeedRound(r.roundNumber, r.author, r.proposedPrice, r.messageContext))
          .toList();
      String name = products.findById(n.productId).map(p -> p.name).orElse("?");
      return new FeedService.FeedRow(
          n.negotiationId, n.productId, name, n.quantity, n.currentFreebiesCost,
          n.currentFreeShipping, n.status, n.lastActor, n.currentRound,
          n.currentPrice, n.updatedAt == null ? 0L : n.updatedAt.toEpochMilli(), rs);
    }).toList();
  }
}
