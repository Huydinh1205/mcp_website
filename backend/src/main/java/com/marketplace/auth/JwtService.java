package com.marketplace.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Mints and verifies the login JWT. */
@Service
public class JwtService {

  public record Principal(String userId, String role) {}

  private final SecretKey key;

  public JwtService(
      @Value("${app.jwt-secret:dev-only-jwt-secret-change-me-please-32b+}") String secret) {
    byte[] material = secret.getBytes(StandardCharsets.UTF_8);
    if (material.length < 32) {
      // stretch short secrets to a valid HS256 key size
      material = sha256(material);
    }
    this.key = Keys.hmacShaKeyFor(material);
  }

  public String mint(String userId, String role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId)
        .claim("role", role)
        .issuedAt(java.util.Date.from(now))
        .expiration(java.util.Date.from(now.plus(7, ChronoUnit.DAYS)))
        .signWith(key)
        .compact();
  }

  /** Returns the principal, or null if the token is missing/invalid/expired. */
  public Principal parse(String token) {
    if (token == null || token.isBlank()) return null;
    try {
      Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      String role = c.get("role", String.class);
      String sub = c.getSubject();
      if (sub == null || role == null) return null;
      // Users are identified by a numeric id in this build. A non-numeric subject
      // (e.g. "B-001") is a stale token from an older id scheme — reject it so the
      // client clears the session and re-authenticates, instead of the id blowing
      // up later as a NumberFormatException deep in the negotiation layer.
      if (!sub.matches("\\d+")) return null;
      return new Principal(sub, role);
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static byte[] sha256(byte[] in) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(in);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
