package com.marketplace.web;

import com.marketplace.AppProps;
import com.marketplace.DiscountService;
import com.marketplace.MarketplaceReads;
import com.marketplace.SellerResponderService;
import com.marketplace.db.ProductRepo;
import com.marketplace.negotiation.*;
import com.marketplace.auth.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transport for the WebMCP tools. The browser tool handlers POST
 * { tool, args, session } here. Every state change goes through
 * NegotiationService.commitTurn -> OffersService.applyTurn (Constitution IV).
 *
 * Offers/counters carry a full DEAL (price + quantity + freebies + free
 * shipping), not just a number — that is what makes an agent worthwhile.
 * Coupons are applied at confirmation, not per turn.
 */
@RestController
public class McpController {

  private final MarketplaceReads reads;
  private final NegotiationService negotiations;
  private final SellerResponderService sellerResponder;
  private final DiscountService discounts;
  private final ProductRepo products;
  private final AppProps props;

  public McpController(MarketplaceReads reads, NegotiationService negotiations,
      SellerResponderService sellerResponder, DiscountService discounts,
      ProductRepo products, AppProps props) {
    this.reads = reads;
    this.negotiations = negotiations;
    this.sellerResponder = sellerResponder;
    this.discounts = discounts;
    this.products = products;
    this.props = props;
  }

  private boolean serverSeller() {
    return !"browser".equalsIgnoreCase(props.sellerMode());
  }

  /** Resolve freebie product ids -> DealTerms with names + cost-to-seller. */
  private DealTerms buildTerms(Map<String, Object> args, double price) {
    int qty = Math.max(1, intOr(args.get("quantity"), 1));
    boolean freeShip = Boolean.parseBoolean(String.valueOf(args.getOrDefault("free_shipping", "false")));
    List<String> ids = new ArrayList<>();
    Object f = args.get("freebies");
    if (f instanceof List<?> list) for (Object o : list) if (o != null) ids.add(String.valueOf(o));

    List<String> names = new ArrayList<>();
    double cost = 0;
    for (String id : ids) {
      var p = products.findById(Long.valueOf(id)).orElse(null);
      if (p != null) {
        names.add(p.name);
        cost += p.minPrice;
      }
    }
    boolean plain = qty == 1 && !freeShip && names.isEmpty();
    return plain ? null
        : new DealTerms(price, qty, names, Math.round(cost * 100.0) / 100.0, freeShip);
  }

  @SuppressWarnings("unchecked")
  @PostMapping("/api/mcp")
  public ResponseEntity<?> dispatch(@RequestBody Map<String, Object> body) {
    String tool = String.valueOf(body.get("tool"));
    Map<String, Object> args = (Map<String, Object>) body.getOrDefault("args", Map.of());
    // Identity comes from the JWT, never the request body (was an IDOR hole).
    String buyerId = CurrentUser.isBuyer() ? CurrentUser.id() : null;
    String sellerId = CurrentUser.isSeller() ? CurrentUser.id() : null;

    try {
      switch (tool) {
        case "search_products":
          return ok(reads.searchProducts(str(args.get("query")), dbl(args.get("max_price")),
              dbl(args.get("min_seller_rating")), str(args.get("category"))));

        case "get_product": {
          if (buyerId == null) return badSession();
          var p = reads.getProduct(str(args.get("product_id")), buyerId);
          return p == null ? notFound() : ok(p);
        }

        case "list_my_offers":
          if (buyerId == null) return badSession();
          return ok(reads.listBuyerNegotiations(buyerId));

        case "list_addons":
          return ok(reads.addonsFor(str(args.get("product_id"))));

        case "list_coupons":
          return ok(discounts.couponsFor(str(args.get("product_id"))));

        case "apply_coupon": {
          if (buyerId == null) return badSession();
          String negId = str(args.get("negotiation_id"));
          if (!negotiations.buyerOwns(negId, buyerId)) return notFound();
          var st = reads.negotiationState(negId);
          if (st.get("error") != null) return notFound();
          double base = ((Number) st.get("current_price")).doubleValue();
          var r = discounts.applyCoupon(negId, str(args.get("code")), base, str(st.get("product_id")));
          if (!r.ok()) return ResponseEntity.status(422).body(Map.of("error", r.error()));
          return ok(Map.of(
              "code", r.code(), "base_price", r.basePrice(),
              "discount", r.discount(), "effective_price", r.effectivePrice()));
        }

        case "submit_offer": {
          if (buyerId == null) return badSession();
          String productId = str(args.get("product_id"));
          var openNeg = negotiations.findOpenForBuyer(buyerId, productId);
          if (openNeg.isPresent()) {
            // Already negotiating this item. Hand back the live state (marked
            // already_open) so the agent continues it with counter_offer /
            // accept_offer instead of dead-ending on an error and looking stuck.
            var state = reads.negotiationState(String.valueOf(openNeg.get().id));
            state.put("already_open", true);
            return ok(state);
          }
          double price = dbl(args.get("price")) == null ? Double.NaN : dbl(args.get("price"));
          var n = negotiations.create(buyerId, productId, intOr(args.get("quantity"), 1));
          String newId = String.valueOf(n.id);
          var res = negotiations.commitTurn(newId, new TurnInput(
              Side.BUYER, TurnAction.OFFER, price, n.currentRound, str(args.get("message")),
              buildTerms(args, price)));
          if (!res.ok()) return ResponseEntity.status(422).body(Map.of("error", res.error().name()));
          if (serverSeller()) sellerResponder.respond(newId);
          return ok(reads.negotiationState(newId));
        }

        case "counter_offer": {
          if (buyerId == null) return badSession();
          String negId = str(args.get("negotiation_id"));
          if (!negotiations.buyerOwns(negId, buyerId)) return notFound();
          double price = dbl(args.get("price")) == null ? Double.NaN : dbl(args.get("price"));
          var res = negotiations.commitTurn(negId, new TurnInput(
              Side.BUYER, TurnAction.COUNTER, price, intOr(args.get("round_seen"), -1),
              str(args.get("message")), buildTerms(args, price)));
          if (!res.ok()) return ResponseEntity.status(422).body(Map.of("error", res.error().name()));
          if (serverSeller()) sellerResponder.respond(negId);
          return ok(reads.negotiationState(negId));
        }

        case "accept_offer": {
          if (buyerId == null) return badSession();
          String negId = str(args.get("negotiation_id"));
          if (!negotiations.buyerOwns(negId, buyerId)) return notFound();
          var res = negotiations.commitTurn(negId, new TurnInput(
              Side.BUYER, TurnAction.ACCEPT, null, intOr(args.get("round_seen"), -1), null));
          if (!res.ok()) return ResponseEntity.status(422).body(Map.of("error", res.error().name()));
          // Server-seller mode: the seller is pre-authorised, so confirm its side
          // as soon as the buyer accepts — the deal then only awaits the human.
          if (serverSeller()) sellerResponder.respond(negId);
          return ok(Map.of(
              "negotiation_id", negId,
              "status", res.status().wire(),
              "requires_human_confirmation", res.requiresHumanConfirmation(),
              "confirm_token", res.confirmToken()));
        }

        case "list_incoming_offers":
          if (sellerId == null) return badSession();
          return ok(reads.listIncomingOffers(sellerId));

        case "get_offer_history": {
          String negId = str(args.get("negotiation_id"));
          if (!negotiations.sellerOwns(negId, sellerId)) return notFound();
          var h = reads.getOfferHistory(negId);
          return h == null ? notFound() : ok(h);
        }

        case "respond_to_offer": {
          if (sellerId == null) return badSession();
          String negId = str(args.get("negotiation_id"));
          if (!negotiations.sellerOwns(negId, sellerId)) return notFound();
          String action = str(args.get("action"));
          TurnAction ta = switch (action == null ? "" : action) {
            case "accept" -> TurnAction.ACCEPT;
            case "reject" -> TurnAction.REJECT;
            default -> TurnAction.COUNTER;
          };
          Double price = ta == TurnAction.COUNTER ? dbl(args.get("price")) : null;
          DealTerms terms = ta == TurnAction.COUNTER && price != null
              ? buildTerms(args, price) : null;
          var res = negotiations.commitTurn(negId, new TurnInput(
              Side.SELLER, ta, price, intOr(args.get("round_seen"), -1),
              str(args.get("message")), terms));
          if (!res.ok()) return ResponseEntity.status(422).body(Map.of("error", res.error().name()));
          var out = new java.util.HashMap<String, Object>();
          out.put("negotiation_id", negId);
          out.put("status", res.status().wire());
          if (res.requiresHumanConfirmation()) {
            out.put("requires_human_confirmation", true);
            out.put("confirm_token", res.confirmToken());
          }
          return ok(out);
        }

        default:
          return ResponseEntity.badRequest().body(Map.of("error", "unknown_tool"));
      }
    } catch (Exception e) {
      return ResponseEntity.status(500)
          .body(Map.of("error", "server_error", "detail", String.valueOf(e.getMessage())));
    }
  }

  private static ResponseEntity<Object> ok(Object b) { return ResponseEntity.ok(b); }
  private static ResponseEntity<Object> notFound() {
    return ResponseEntity.status(404).body(Map.of("error", "NOT_FOUND"));
  }
  private static ResponseEntity<Object> badSession() {
    return ResponseEntity.badRequest().body(Map.of("error", "no_session"));
  }
  private static String str(Object o) {
    return o == null || String.valueOf(o).isEmpty() ? (o == null ? null : String.valueOf(o)) : String.valueOf(o);
  }
  private static Double dbl(Object o) {
    if (o == null || String.valueOf(o).isBlank()) return null;
    try { return Double.parseDouble(String.valueOf(o)); } catch (NumberFormatException e) { return null; }
  }
  private static int intOr(Object o, int def) {
    if (o == null) return def;
    try { return (int) Math.round(Double.parseDouble(String.valueOf(o))); } catch (NumberFormatException e) { return def; }
  }
}
