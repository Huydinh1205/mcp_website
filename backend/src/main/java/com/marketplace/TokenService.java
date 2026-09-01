package com.marketplace;

import com.marketplace.negotiation.Side;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Confirm tokens for the human-in-the-loop gate (D-A4, FR-011). Port of
 * lib/tokens.ts. {@code <payloadB64>.<hmacSha256Base64Url>}.
 */
@Service
public class TokenService {

  public record Payload(String negotiationId, Side side) {}

  private final String secret;

  @Autowired
  public TokenService(AppProps props) {
    this(props.confirmSecret() == null ? "dev-only-confirm-secret-change-me" : props.confirmSecret());
  }

  public TokenService(String secret) {
    this.secret = secret;
  }

  public String mint(String negotiationId, Side side) {
    String payload = b64Url(("{\"negotiationId\":\"" + esc(negotiationId)
        + "\",\"side\":\"" + side.wire() + "\"}").getBytes(StandardCharsets.UTF_8));
    return payload + "." + sign(payload);
  }

  public Payload verify(String token) {
    if (token == null) return null;
    String[] parts = token.split("\\.");
    if (parts.length != 2) return null;

    byte[] expected = sign(parts[0]).getBytes(StandardCharsets.UTF_8);
    byte[] given = parts[1].getBytes(StandardCharsets.UTF_8);
    if (!MessageDigest.isEqual(expected, given)) return null;

    try {
      String json = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
      String negId = extract(json, "negotiationId");
      String side = extract(json, "side");
      if (negId == null || side == null) return null;
      if (!side.equals("buyer") && !side.equals("seller")) return null;
      return new Payload(negId, Side.fromWire(side));
    } catch (RuntimeException e) {
      return null;
    }
  }

  private String sign(String payloadB64) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return b64Url(mac.doFinal(payloadB64.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static String b64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String esc(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  /** Minimal string-value extractor for our own tiny fixed-shape JSON. */
  private static String extract(String json, String key) {
    String needle = "\"" + key + "\":\"";
    int i = json.indexOf(needle);
    if (i < 0) return null;
    int start = i + needle.length();
    int end = json.indexOf('"', start);
    return end < 0 ? null : json.substring(start, end);
  }
}
