package com.marketplace.auth;

import org.springframework.security.core.context.SecurityContextHolder;

/** Reads the authenticated principal set by JwtAuthFilter. */
public final class CurrentUser {
  private CurrentUser() {}

  public static String id() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a == null ? null : (String) a.getPrincipal();
  }

  public static String role() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) return null;
    return a.getAuthorities().stream().findFirst()
        .map(x -> x.getAuthority().replace("ROLE_", "").toLowerCase())
        .orElse(null);
  }

  public static boolean isBuyer() { return "buyer".equals(role()); }
  public static boolean isSeller() { return "seller".equals(role()); }
}
