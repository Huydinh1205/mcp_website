package com.marketplace;

import com.marketplace.auth.JwtService;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Attach a Bearer token for a seeded user in MockMvc tests. */
public final class TestAuth {
  private TestAuth() {}

  public static RequestPostProcessor as(JwtService jwt, String userId, String role) {
    String token = jwt.mint(userId, role);
    return request -> {
      request.addHeader("Authorization", "Bearer " + token);
      return request;
    };
  }
  public static RequestPostProcessor buyer(JwtService jwt, String id) { return as(jwt, id, "buyer"); }
  public static RequestPostProcessor seller(JwtService jwt, String id) { return as(jwt, id, "seller"); }
}
