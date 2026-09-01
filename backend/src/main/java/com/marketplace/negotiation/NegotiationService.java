package com.marketplace.negotiation;

import com.marketplace.FeedService;
import com.marketplace.TokenService;
import com.marketplace.db.*;
import java.util.List;
import java.util.Optional;
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
        lastSeller);
  }

  @Transactional
  public CommitResult commitTurn(String negotiationId, TurnInput input) {
    NegotiationSnapshot snap = loadSnapshot(negotiationId);
    if (snap == null) return CommitResult.err(TurnErrorCode.NOT_FOUND);

    TurnResult r = offers.applyTurn(snap, input);
    if (r instanceof TurnResult.Err e) return CommitResult.err(e.error());
    TurnOutcome o = ((TurnResult.Ok) r).value();

    int written = negotiations.applyTurn(
        negotiationId,
        java.time.Instant.now(),
        snap.currentRound(),
        o.nextStatus().wire(),
        o.nextLastActor().wire(),
        o.nextRound(),
        o.nextPrice());
    if (written == 0) return CommitResult.err(TurnErrorCode.STALE);

    if (o.appendRound() != null) {
      NegotiationRoundEntity nr = new NegotiationRoundEntity();
      nr.negotiationId = negotiationId;
      nr.roundNumber = o.nextRound();
      nr.proposedPrice = o.appendRound().proposedPrice();
      nr.messageContext = o.appendRound().message();
      nr.author = o.appendRound().author().wire();
      rounds.save(nr);
    }

    String token =
        o.requiresHumanConfirmation() ? tokens.mint(negotiationId, input.actor()) : null;
    return CommitResult.ok(o.nextStatus(), o.requiresHumanConfirmation(), token);
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

  public List<FeedService.FeedRow> feedRows(String buyerIdOrNull) {
    List<NegotiationEntity> ns =
        buyerIdOrNull == null ? negotiations.findAll() : negotiations.findByNationalId(buyerIdOrNull);
    return ns.stream().map(n -> {
      var rs = rounds.findByNegotiationIdOrderByRoundNumberAsc(n.negotiationId).stream()
          .map(r -> new FeedService.FeedRound(r.roundNumber, r.author, r.proposedPrice, r.messageContext))
          .toList();
      return new FeedService.FeedRow(
          n.negotiationId, n.status, n.lastActor, n.currentRound, n.currentPrice,
          n.updatedAt == null ? 0L : n.updatedAt.toEpochMilli(), rs);
    }).toList();
  }
}
