package com.marketplace.web;

import com.marketplace.AppProps;
import com.marketplace.MarketplaceReads;
import com.marketplace.SellerResponderService;
import com.marketplace.negotiation.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transport for the WebMCP tools. The browser tool handlers POST
 * { tool, args, session } here. Every state change goes through
 * NegotiationService.commitTurn -> OffersService.applyTurn (Constitution IV).
 */
@RestController
public class McpController {

  private final MarketplaceReads reads;
  private final NegotiationService negotiations;
  private final SellerResponderService sellerResponder;
  private final AppProps props;

  public McpController(MarketplaceReads reads, NegotiationService negotiations,
      SellerResponderService sellerResponder, AppProps props) {
    this.reads = reads;
    this.negotiations = negotiations;
    this.sellerResponder = sellerResponder;
    this.props = props;
  }

  private boolean serverSeller() {
    return !"browser".equalsIgnoreCase(props.sellerMode());
  }

  @SuppressWarnings("unchecked")
  @PostMapping("/api/mcp")
  public ResponseEntity<?> dispatch(@RequestBody Map<String, Object> body) {
    String tool = String.valueOf(body.get("tool"));
    Map<String, Object> args = (Map<String, Object>) body.getOrDefault("args", Map.of());
    Map<String, Object> session = (Map<String, Object>) body.getOrDefault("session", Map.of());
    String buyerId = str(session.get("buyerId"));
    String sellerId = str(session.get("sellerId"));

    try {
      switch (tool) {
        case "search_products":
          return ok(reads.searchProducts(str(args.get("query")), dbl(args.get("max_price")),
              dbl(args.get("min_seller_rating"))));

        case "get_product": {
          if (buyerId == null) return badSession();
          var p = reads.getProduct(str(args.get("product_id")), buyerId);
          return p == null ? notFound() : ok(p);
        }

        case "list_my_offers":
          if (buyerId == null) return badSession();
          return ok(reads.listBuyerNegotiations(buyerId));

        case "submit_offer": {
          if (buyerId == null) return badSession();
          String productId = str(args.get("product_id"));
          if (negotiations.findOpenForBuyer(buyerId, productId).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "ALREADY_OPEN"));
          }
          var n = negotiations.create(buyerId, productId, intOr(args.get("quantity"), 1));
          var res = negotiations.commitTurn(n.negotiationId, new TurnInput(
              Side.BUYER, TurnAction.OFFER, dbl(args.get("price")), 0, str(args.get("message"))));
          if (!res.ok()) return ResponseEntity.status(422).body(Map.of("error", res.error().name()));
          if (serverSeller()) sellerResponder.respond(n.negotiationId);
          return ok(reads.negotiationState(n.negotiationId));
        }

        case "counter_offer": {
          if (buyerId == null) return badSession();
          String negId = str(args.get("negotiation_id"));
          var res = negotiations.commitTurn(negId, new TurnInput(
              Side.BUYER, TurnAction.COUNTER, dbl(args.get("price")),
              intOr(args.get("round_seen"), -1), str(args.get("message"))));
          if (!res.ok()) return ResponseEntity.status(422).body(Map.of("error", res.error().name()));
          if (serverSeller()) sellerResponder.respond(negId);
          return ok(reads.negotiationState(negId));
        }

        case "accept_offer": {
          if (buyerId == null) return badSession();
          String negId = str(args.get("negotiation_id"));
          var res = negotiations.commitTurn(negId, new TurnInput(
              Side.BUYER, TurnAction.ACCEPT, null, intOr(args.get("round_seen"), -1), null));
          if (!res.ok()) return ResponseEntity.status(422).body(Map.of("error", res.error().name()));
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
          var h = reads.getOfferHistory(str(args.get("negotiation_id")));
          return h == null ? notFound() : ok(h);
        }

        case "respond_to_offer": {
          String negId = str(args.get("negotiation_id"));
          String action = str(args.get("action"));
          TurnAction ta = switch (action == null ? "" : action) {
            case "accept" -> TurnAction.ACCEPT;
            case "reject" -> TurnAction.REJECT;
            default -> TurnAction.COUNTER;
          };
          var res = negotiations.commitTurn(negId, new TurnInput(
              Side.SELLER, ta, ta == TurnAction.COUNTER ? dbl(args.get("price")) : null,
              intOr(args.get("round_seen"), -1), str(args.get("message"))));
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
      return ResponseEntity.status(500).body(Map.of("error", "server_error", "detail", String.valueOf(e.getMessage())));
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
