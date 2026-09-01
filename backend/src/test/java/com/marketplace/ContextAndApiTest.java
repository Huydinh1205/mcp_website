package com.marketplace;

import static com.marketplace.TestAuth.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketplace.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Full context + web + security wire correctly, and SeedRunner populated demo data. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContextAndApiTest {

  @Autowired MockMvc mvc;
  @Autowired JwtService jwt;

  private String body(org.springframework.test.web.servlet.ResultActions ra) throws Exception {
    return ra.andReturn().getResponse().getContentAsString();
  }

  @Test
  void seededUserLogsInAndFetchesMe() throws Exception {
    String login = body(mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"email\":\"mai@example.com\",\"password\":\"password\"}"))
        .andExpect(status().isOk()));
    String token = login.split("\"token\":\"")[1].split("\"")[0];
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("buyer"));
  }

  @Test
  void mcpRequiresAuth() throws Exception {
    mvc.perform(post("/api/mcp").contentType("application/json")
            .content("{\"tool\":\"search_products\",\"args\":{\"query\":\"x\"}}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void searchProductsToolReturnsCatalog() throws Exception {
    mvc.perform(post("/api/mcp").with(buyer(jwt, "B-001")).contentType("application/json")
            .content("{\"tool\":\"search_products\",\"args\":{\"query\":\"keyboard\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].product_id").exists());
  }

  @Test
  void feedEndpointResponds() throws Exception {
    mvc.perform(get("/api/negotiations").param("since", "").with(buyer(jwt, "B-001")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cursor").exists())
        .andExpect(jsonPath("$.negotiations").isArray());
  }

  @Test
  void submitOffer_runsServerSellerResponder_andReachesADeal() throws Exception {
    String search = body(mvc.perform(post("/api/mcp").with(buyer(jwt, "B-001"))
        .contentType("application/json")
        .content("{\"tool\":\"search_products\",\"args\":{\"query\":\"65% Mechanical Keyboard\"}}")));
    String productId = search.split("\"product_id\":\"")[1].split("\"")[0];

    String state = body(mvc.perform(post("/api/mcp").with(buyer(jwt, "B-001"))
        .contentType("application/json")
        .content("{\"tool\":\"submit_offer\",\"args\":{\"product_id\":\"" + productId + "\",\"price\":40}}"))
        .andExpect(status().isOk()));

    assertThat(state).contains("\"status\":");
    assertThat(state).doesNotContain("\"status\":\"open\"");
  }
}
