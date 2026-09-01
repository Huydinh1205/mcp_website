package com.marketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static com.marketplace.TestAuth.*;
import com.marketplace.auth.JwtService;

/** A negotiation belongs to one buyer / one seller — nobody else can touch it. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdorGuardsTest {

  @Autowired MockMvc mvc;
  @Autowired JwtService jwt;

  private String body(org.springframework.test.web.servlet.ResultActions ra) throws Exception {
    return ra.andReturn().getResponse().getContentAsString();
  }

  private String openNegotiationAs(String buyerId, String query) throws Exception {
    String s = body(mvc.perform(post("/api/mcp").with(buyer(jwt, buyerId)).contentType("application/json")
        .content("{\"tool\":\"search_products\",\"args\":{\"query\":\"" + query + "\"}}")));
    String pid = s.split("\"product_id\":\"")[1].split("\"")[0];
    String st = body(mvc.perform(post("/api/mcp").with(buyer(jwt, buyerId)).contentType("application/json")
        .content("{\"tool\":\"submit_offer\","
            + "\"args\":{\"product_id\":\"" + pid + "\",\"price\":18}}")));
    return st.split("\"negotiation_id\":\"")[1].split("\"")[0];
  }

  @Test
  void anotherBuyerCannotCounterOrCouponAForeignNegotiation() throws Exception {
    String negId = openNegotiationAs("B-001", "Palm Rest");

    mvc.perform(post("/api/mcp").with(buyer(jwt, "B-002")).contentType("application/json")
        .content("{\"tool\":\"counter_offer\","
            + "\"args\":{\"negotiation_id\":\"" + negId + "\",\"price\":20,\"round_seen\":1}}"))
        .andExpect(status().isNotFound());

    mvc.perform(post("/api/mcp").with(buyer(jwt, "B-002")).contentType("application/json")
        .content("{\"tool\":\"apply_coupon\","
            + "\"args\":{\"negotiation_id\":\"" + negId + "\",\"code\":\"WELCOME10\"}}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void aSellerCannotRespondToANegotiationForAnotherSellersProduct() throws Exception {
    String negId = openNegotiationAs("B-003", "Numpad Module"); // S-003's product

    mvc.perform(post("/api/mcp").with(seller(jwt, "S-001")).contentType("application/json")
        .content("{\"tool\":\"respond_to_offer\","
            + "\"args\":{\"negotiation_id\":\"" + negId + "\",\"action\":\"accept\",\"round_seen\":1}}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void reviewIsRejectedForANegotiationYouDoNotOwn() throws Exception {
    String negId = openNegotiationAs("B-001", "Travel Keyboard Bag");
    mvc.perform(post("/api/feedback").with(buyer(jwt, "B-002")).contentType("application/json")
        .content("{\"product_id\":\"x\",\"negotiation_id\":\"" + negId
            + "\",\"rating\":5}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }
}
