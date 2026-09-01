package com.marketplace.auth;

import com.marketplace.db.*;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Register / login. Passwords are BCrypt-hashed; login returns a JWT. */
@Service
public class AuthService {

  private final UserRepo users;
  private final BuyerRepo buyers;
  private final SellerRepo sellers;
  private final BuyerConfigRepo buyerConfigs;
  private final JwtService jwt;
  private final BCryptPasswordEncoder encoder;

  public AuthService(UserRepo users, BuyerRepo buyers, SellerRepo sellers,
      BuyerConfigRepo buyerConfigs, JwtService jwt, BCryptPasswordEncoder encoder) {
    this.users = users;
    this.buyers = buyers;
    this.sellers = sellers;
    this.buyerConfigs = buyerConfigs;
    this.jwt = jwt;
    this.encoder = encoder;
  }

  public record RegisterResult(String userId, String token, String role, String error) {}
  public record LoginResult(String userId, String token, String role, String error) {}

  @Transactional
  public RegisterResult register(String name, String email, String password, String role) {
    if (!"buyer".equals(role) && !"seller".equals(role))
      return new RegisterResult(null, null, null, "BAD_ROLE");
    if (email == null || password == null || password.length() < 6)
      return new RegisterResult(null, null, null, "BAD_INPUT");
    if (users.findByEmailIgnoreCase(email).isPresent())
      return new RegisterResult(null, null, null, "EMAIL_TAKEN");

    String[] parts = name == null || name.isBlank() ? new String[] {"New", "User"} : name.trim().split("\\s+", 2);
    String id = UUID.randomUUID().toString().replace("-", "");

    UserEntity u = new UserEntity();
    u.nationalId = id;
    u.firstName = parts[0];
    u.lastName = parts.length > 1 ? parts[1] : "";
    u.email = email;
    u.role = role;
    u.passwordHash = encoder.encode(password);
    users.save(u);

    if ("buyer".equals(role)) {
      BuyerEntity b = new BuyerEntity();
      b.nationalId = id;
      b.interest = "";
      buyers.save(b);
      BuyerAiConfigEntity cfg = new BuyerAiConfigEntity();
      cfg.buyerAgentId = UUID.randomUUID().toString().replace("-", "");
      cfg.nationalId = id;
      cfg.maxBudget = 200;
      cfg.targetPrice = 120;
      cfg.minSellerRating = 0;
      cfg.style = "fair";
      buyerConfigs.save(cfg);
    } else {
      SellerEntity s = new SellerEntity();
      s.nationalId = id;
      s.rating = 4.0;
      sellers.save(s);
    }

    return new RegisterResult(id, jwt.mint(id, role), role, null);
  }

  public LoginResult login(String email, String password) {
    var u = email == null ? null : users.findByEmailIgnoreCase(email).orElse(null);
    if (u == null || u.passwordHash == null || !encoder.matches(password, u.passwordHash))
      return new LoginResult(null, null, null, "BAD_CREDENTIALS");
    return new LoginResult(u.nationalId, jwt.mint(u.nationalId, u.role), u.role, null);
  }
}
