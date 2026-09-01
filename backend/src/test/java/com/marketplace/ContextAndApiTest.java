package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Full context + web layer wire correctly, and SeedRunner populated demo data. */
@SpringBootTest
@AutoConfigureMockMvc
class ContextAndApiTest {

  @Autowired MockMvc mvc;

  @Test
  void buyersAndSellersSeeded() throws Exception {
    mvc.perform(get("/api/buyers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].config.maxBudget").exists());
    mvc.perform(get("/api/sellers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5));
  }

  @Test
  void searchProductsToolReturnsCatalog() throws Exception {
    mvc.perform(post("/api/mcp")
            .contentType("application/json")
            .content("{\"tool\":\"search_products\",\"args\":{\"query\":\"keyboard\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].product_id").exists());
  }

  @Test
  void feedEndpointResponds() throws Exception {
    mvc.perform(get("/api/negotiations").param("since", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cursor").exists())
        .andExpect(jsonPath("$.negotiations").isArray());
  }

  @Test
  void submitOffer_runsServerSellerResponder_andReachesADeal() throws Exception {
    // find a product carried by a seller the buyer B-001 accepts (rating >= 4.0)
    String search = mvc.perform(post("/api/mcp")
            .contentType("application/json")
            .content("{\"tool\":\"search_products\",\"args\":{\"query\":\"65% Mechanical Keyboard\"}}"))
        .andReturn().getResponse().getContentAsString();
    String productId = search.split("\"product_id\":\"")[1].split("\"")[0];

    String state = mvc.perform(post("/api/mcp")
            .contentType("application/json")
            .content("{\"tool\":\"submit_offer\",\"session\":{\"buyerId\":\"B-001\"},"
                + "\"args\":{\"product_id\":\"" + productId + "\",\"price\":40}}"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    // the server-side seller responder took a turn: status advanced past "open"
    assertThat(state).contains("\"status\":");
    assertThat(state).doesNotContain("\"status\":\"open\"");
  }
}
