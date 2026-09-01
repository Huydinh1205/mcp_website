package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.negotiation.Side;
import org.junit.jupiter.api.Test;

/** Port of lib/tokens.test.ts */
class TokenServiceTest {

  private final TokenService tokens = new TokenService("test-secret");

  @Test
  void roundTrips() {
    String t = tokens.mint("neg_123", Side.BUYER);
    TokenService.Payload p = tokens.verify(t);
    assertThat(p).isNotNull();
    assertThat(p.negotiationId()).isEqualTo("neg_123");
    assertThat(p.side()).isEqualTo(Side.BUYER);
  }

  @Test
  void buyerAndSellerTokensAreDistinct() {
    String b = tokens.mint("neg_1", Side.BUYER);
    String s = tokens.mint("neg_1", Side.SELLER);
    assertThat(b).isNotEqualTo(s);
    assertThat(tokens.verify(s).side()).isEqualTo(Side.SELLER);
  }

  @Test
  void tamperedTokenFails() {
    String t = tokens.mint("neg_123", Side.BUYER);
    String tampered = t.substring(0, t.length() - 2) + (t.endsWith("A") ? "B" : "A");
    assertThat(tokens.verify(tampered)).isNull();
  }

  @Test
  void wrongSecretFails() {
    String t = tokens.mint("neg_123", Side.BUYER);
    assertThat(new TokenService("other").verify(t)).isNull();
  }

  @Test
  void garbageReturnsNullNeverThrows() {
    assertThat(tokens.verify("not-a-token")).isNull();
    assertThat(tokens.verify("")).isNull();
    assertThat(tokens.verify("a.b.c.d")).isNull();
  }
}
