package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.auth.AuthService;
import com.marketplace.auth.JwtService;
import com.marketplace.db.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DataJpaTest
@Import({AuthService.class, JwtService.class, BCryptPasswordEncoder.class})
class AuthTest {

  @Autowired AuthService auth;
  @Autowired JwtService jwt;
  @Autowired UserRepo users;
  @Autowired BuyerRepo buyers;
  @Autowired SellerRepo sellers;

  @Test
  void registerBuyer_createsUserBuyerAndConfig_thenLoginReturnsAToken() {
    var r = auth.register("Nam Le", "nam@example.com", "secret123", "buyer");
    assertThat(r.error()).isNull();
    assertThat(users.findByEmailIgnoreCase("nam@example.com")).isPresent();
    assertThat(buyers.findById(r.userId())).isPresent();

    var login = auth.login("nam@example.com", "secret123");
    assertThat(login.error()).isNull();
    var claims = jwt.parse(login.token());
    assertThat(claims.userId()).isEqualTo(r.userId());
    assertThat(claims.role()).isEqualTo("buyer");
  }

  @Test
  void registerSeller_createsSellerRow() {
    var r = auth.register("Kho A", "khoa@example.com", "secret123", "seller");
    assertThat(r.error()).isNull();
    assertThat(sellers.findById(r.userId())).isPresent();
  }

  @Test
  void duplicateEmail_rejected() {
    auth.register("A", "dup@example.com", "secret123", "buyer");
    var r2 = auth.register("B", "DUP@example.com", "secret123", "buyer");
    assertThat(r2.error()).isEqualTo("EMAIL_TAKEN");
  }

  @Test
  void wrongPassword_rejected() {
    auth.register("A", "wp@example.com", "secret123", "buyer");
    assertThat(auth.login("wp@example.com", "nope").error()).isEqualTo("BAD_CREDENTIALS");
  }

  @Test
  void badRole_rejected() {
    assertThat(auth.register("A", "br@example.com", "secret123", "admin").error())
        .isEqualTo("BAD_ROLE");
  }

  @Test
  void tamperedToken_failsToParse() {
    var r = auth.register("A", "tk@example.com", "secret123", "buyer");
    String tok = auth.login("tk@example.com", "secret123").token();
    assertThat(jwt.parse(tok.substring(0, tok.length() - 3) + "xxx")).isNull();
  }
}
