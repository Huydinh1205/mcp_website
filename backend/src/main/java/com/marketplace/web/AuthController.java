package com.marketplace.web;

import com.marketplace.auth.AuthService;
import com.marketplace.auth.CurrentUser;
import com.marketplace.db.UserRepo;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService auth;
  private final UserRepo users;

  public AuthController(AuthService auth, UserRepo users) {
    this.auth = auth;
    this.users = users;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody Map<String, Object> b) {
    var r = auth.register(str(b.get("name")), str(b.get("email")),
        str(b.get("password")), str(b.get("role")));
    if (r.error() != null) return ResponseEntity.badRequest().body(Map.of("error", r.error()));
    return ResponseEntity.ok(Map.of("token", r.token(), "role", r.role(), "userId", r.userId()));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody Map<String, Object> b) {
    var r = auth.login(str(b.get("email")), str(b.get("password")));
    if (r.error() != null) return ResponseEntity.status(401).body(Map.of("error", r.error()));
    return ResponseEntity.ok(Map.of("token", r.token(), "role", r.role(), "userId", r.userId()));
  }

  @GetMapping("/me")
  public ResponseEntity<?> me() {
    String id = CurrentUser.id();
    if (id == null) return ResponseEntity.status(401).body(Map.of("error", "UNAUTHENTICATED"));
    var u = users.findById(id).orElse(null);
    if (u == null) return ResponseEntity.status(401).body(Map.of("error", "UNKNOWN_USER"));
    return ResponseEntity.ok(Map.of(
        "userId", u.nationalId, "name", (u.firstName + " " + u.lastName).trim(),
        "email", u.email, "role", CurrentUser.role()));
  }

  private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
