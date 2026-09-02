package com.marketplace.web;

import com.marketplace.auth.AuthService;
import com.marketplace.auth.CurrentUser;
import com.marketplace.db.UserEntity;
import com.marketplace.db.UserRepo;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final Set<String> AU_STATES =
      Set.of("NSW", "VIC", "QLD", "WA", "SA", "TAS", "ACT", "NT");

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
    var u = users.findById(Long.valueOf(id)).orElse(null);
    if (u == null) return ResponseEntity.status(401).body(Map.of("error", "UNKNOWN_USER"));
    return ResponseEntity.ok(profile(u));
  }

  /** Update display name + shipping address. */
  @Transactional
  @PutMapping("/me")
  public ResponseEntity<?> updateMe(@RequestBody Map<String, Object> b) {
    String id = CurrentUser.id();
    if (id == null) return ResponseEntity.status(401).body(Map.of("error", "UNAUTHENTICATED"));
    var u = users.findById(Long.valueOf(id)).orElse(null);
    if (u == null) return ResponseEntity.status(401).body(Map.of("error", "UNKNOWN_USER"));

    String name = str(b.get("name"));
    if (name != null && !name.isBlank()) {
      String[] p = name.trim().split("\\s+", 2);
      u.firstName = p[0];
      u.lastName = p.length > 1 ? p[1] : "";
    }
    u.phoneNumber = blankToNull(str(b.get("phone")));
    u.streetAddress = blankToNull(str(b.get("street_address")));
    u.suburb = blankToNull(str(b.get("suburb")));

    String state = str(b.get("state"));
    state = state == null ? null : state.trim().toUpperCase();
    if (state != null && !state.isEmpty() && !AU_STATES.contains(state))
      return ResponseEntity.badRequest().body(Map.of("error", "BAD_STATE"));
    u.state = blankToNull(state);

    String pc = str(b.get("postcode"));
    pc = pc == null ? null : pc.trim();
    if (pc != null && !pc.isEmpty() && !pc.matches("\\d{4}"))
      return ResponseEntity.badRequest().body(Map.of("error", "BAD_POSTCODE"));
    u.postcode = blankToNull(pc);

    users.save(u);
    return ResponseEntity.ok(profile(u));
  }

  private Map<String, Object> profile(UserEntity u) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("userId", String.valueOf(u.id));
    m.put("name", (u.firstName + " " + (u.lastName == null ? "" : u.lastName)).trim());
    m.put("email", u.email);
    m.put("role", u.role);
    m.put("phone", u.phoneNumber);
    m.put("street_address", u.streetAddress);
    m.put("suburb", u.suburb);
    m.put("state", u.state);
    m.put("postcode", u.postcode);
    m.put("full_address", u.fullAddress());
    return m;
  }

  private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
  private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
