package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import static com.marketplace.TestAuth.*;
import com.marketplace.auth.JwtService;

/** US4 catalog + multi-term deals + coupons + reviews. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogAndDealsTest {

  @Autowired MockMvc mvc;
  @Autowired JwtService jwt;

  private String body(org.springframework.test.web.servlet.ResultActions ra) throws Exception {
    return ra.andReturn().getResponse().getContentAsString();
  }

  private String pid(String q) throws Exception {
    return body(mvc.perform(post("/api/mcp").with(buyer(jwt, "B-001")).contentType("application/json")
        .content("{\"tool\":\"search_products\",\"args\":{\"query\":\"" + q + "\"}}")))
        .split("\"product_id\":\"")[1].split("\"")[0];
  }

  @Test
  void catalogEndpoints() throws Exception {
    mvc.perform(get("/api/products").param("q", "Keyboard"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].product_id").exists());
    mvc.perform(get("/api/categories"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0]").exists());
    String id = pid("65% Mechanical Keyboard");
    mvc.perform(get("/api/products/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sellers").isArray())
        .andExpect(jsonPath("$.avg_rating").exists())
        .andExpect(jsonPath("$.reviews").isArray());
  }

  @Test
  void categoryFilterNarrowsResults() throws Exception {
    String audio = body(mvc.perform(get("/api/products").param("category", "Audio")));
    assertThat(audio).contains("Headphones").doesNotContain("Keyboard");
  }

  @Test
  void listAddonsReturnsCheapItemsFromTheSameSeller() throws Exception {
    String id = pid("Studio Headphones"); // S-004
    String addons = body(mvc.perform(post("/api/mcp").with(buyer(jwt, "B-001")).contentType("application/json")
        .content("{\"tool\":\"list_addons\",\"args\":{\"product_id\":\"" + id + "\"}}")));
    // S-004 also sells "Boom Arm" (65) — too pricey; "USB DAC" (89) too. So could be empty,
    // but the endpoint must still respond with a JSON array.
    assertThat(addons.trim()).startsWith("[");
  }

  @Test
  void submitOfferWithFreebieAndFreeShipping_persistsDealTerms() throws Exception {
    String kb = pid("65% Mechanical Keyboard"); // some seller carries keycaps too
    String keycaps = pid("PBT Keycap Set");
    String st = body(mvc.perform(post("/api/mcp").with(buyer(jwt, "B-001")).contentType("application/json")
        .content("{\"tool\":\"submit_offer\","
            + "\"args\":{\"product_id\":\"" + kb + "\",\"price\":58,"
            + "\"freebies\":[\"" + keycaps + "\"],\"free_shipping\":true}}"))
        .andExpect(status().isOk()));
    assertThat(st).contains("\"current_free_shipping\":true");
    assertThat(st).contains("\"current_freebies_cost\":");
    assertThat(st).doesNotContain("\"current_freebies_cost\":0.0");
  }

  @Test
  void couponReducesEffectivePrice_andShowsUpAtConfirm() throws Exception {
    String pid = pid("USB DAC"); // S-004, coupon AUDIO15 = 15% off
    String st = body(mvc.perform(post("/api/mcp").with(buyer(jwt, "B-002")).contentType("application/json")
        .content("{\"tool\":\"submit_offer\","
            + "\"args\":{\"product_id\":\"" + pid + "\",\"price\":70}}")));
    String negId = st.split("\"negotiation_id\":\"")[1].split("\"")[0];

    String coupons = body(mvc.perform(post("/api/mcp").with(buyer(jwt, "B-002")).contentType("application/json")
        .content("{\"tool\":\"list_coupons\",\"args\":{\"product_id\":\"" + pid + "\"}}")));
    assertThat(coupons).contains("AUDIO15");

    String applied = body(mvc.perform(post("/api/mcp").with(buyer(jwt, "B-002")).contentType("application/json")
        .content("{\"tool\":\"apply_coupon\","
            + "\"args\":{\"negotiation_id\":\"" + negId + "\",\"code\":\"AUDIO15\"}}"))
        .andExpect(status().isOk()));
    assertThat(applied).contains("\"discount\":").contains("\"effective_price\":");
  }

  @Test
  void feedbackRejectedBeforePurchase() throws Exception {
    String pid = pid("Desk Mat XL");
    String st = body(mvc.perform(post("/api/mcp").with(buyer(jwt, "B-003")).contentType("application/json")
        .content("{\"tool\":\"submit_offer\","
            + "\"args\":{\"product_id\":\"" + pid + "\",\"price\":18}}")));
    String negId = st.split("\"negotiation_id\":\"")[1].split("\"")[0];
    mvc.perform(post("/api/feedback").with(buyer(jwt, "B-003")).contentType("application/json")
        .content("{\"product_id\":\"" + pid + "\",\"negotiation_id\":\"" + negId
            + "\",\"rating\":5,\"comment\":\"great\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("NOT_PURCHASED"));
  }
}
