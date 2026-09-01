package com.marketplace;

import com.marketplace.db.*;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Populates demo data on first boot (when the users table is empty). Port of prisma/seed.ts. */
@Component
public class SeedRunner implements CommandLineRunner {

  private final UserRepo users;
  private final SellerRepo sellers;
  private final BuyerRepo buyers;
  private final ProductRepo products;
  private final SellerConfigRepo sellerConfigs;
  private final BuyerConfigRepo buyerConfigs;
  private final DiscountService discountService;
  private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder;

  public SeedRunner(UserRepo users, SellerRepo sellers, BuyerRepo buyers, ProductRepo products,
      SellerConfigRepo sellerConfigs, BuyerConfigRepo buyerConfigs, DiscountService discountService,
      org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder) {
    this.users = users;
    this.sellers = sellers;
    this.buyers = buyers;
    this.products = products;
    this.sellerConfigs = sellerConfigs;
    this.buyerConfigs = buyerConfigs;
    this.discountService = discountService;
    this.encoder = encoder;
  }

  record S(String id, String first, String last, double rating) {}
  record B(String id, String first, String last, double budget, double target, double minRating,
      String style, String interest) {}
  record C(String name, String category, double price, double min, double auto, double step, List<String> sellers) {}

  @Override
  public void run(String... args) {
    if (users.count() > 0) return;

    var SELLERS = List.of(
        new S("S-001", "KeyLab", "Store", 4.7),
        new S("S-002", "Nordic", "Desk", 4.4),
        new S("S-003", "Budget", "Peripherals", 3.9),
        new S("S-004", "Aurora", "Audio", 4.8),
        new S("S-005", "CablePit", "Supply", 4.1));

    for (var s : SELLERS) {
      UserEntity u = new UserEntity();
      u.nationalId = s.id(); u.firstName = s.first(); u.lastName = s.last();
      u.email = s.first().toLowerCase() + "@example.com";
      u.role = "seller";
      u.passwordHash = encoder.encode("password");
      users.save(u);
      SellerEntity se = new SellerEntity();
      se.nationalId = s.id(); se.rating = s.rating();
      sellers.save(se);
    }

    var BUYERS = List.of(
        new B("B-001", "Mai", "Tran", 60, 46, 4.0, "aggressive", "mechanical keyboards"),
        new B("B-002", "Long", "Pham", 130, 100, 4.3, "fair", "wireless keyboards and audio"),
        new B("B-003", "An", "Nguyen", 30, 18, 3.5, "quick", "cables and accessories"));

    for (var b : BUYERS) {
      UserEntity u = new UserEntity();
      u.nationalId = b.id(); u.firstName = b.first(); u.lastName = b.last();
      u.email = b.first().toLowerCase() + "@example.com";
      u.role = "buyer";
      u.passwordHash = encoder.encode("password");
      users.save(u);
      BuyerEntity be = new BuyerEntity();
      be.nationalId = b.id(); be.interest = b.interest();
      buyers.save(be);
      BuyerAiConfigEntity cfg = new BuyerAiConfigEntity();
      cfg.buyerAgentId = UUID.randomUUID().toString().replace("-", "");
      cfg.nationalId = b.id(); cfg.maxBudget = b.budget(); cfg.targetPrice = b.target();
      cfg.minSellerRating = b.minRating(); cfg.style = b.style();
      buyerConfigs.save(cfg);
    }

    var CATALOG = List.of(
        new C("65% Mechanical Keyboard", "Keyboards", 79, 44, 58, 8, List.of("S-001", "S-002", "S-003")),
        new C("75% Wireless Keyboard", "Keyboards", 119, 74, 95, 10, List.of("S-001", "S-002")),
        new C("Low-Profile Keyboard", "Keyboards", 99, 60, 80, 9, List.of("S-001", "S-003")),
        new C("USB-C Coiled Cable", "Cables", 25, 9, 15, 3, List.of("S-005", "S-003")),
        new C("Aluminium Keyboard Case", "Keyboards", 45, 26, 34, 5, List.of("S-002")),
        new C("PBT Keycap Set", "Keyboards", 55, 30, 42, 6, List.of("S-001", "S-003")),
        new C("Desk Mat XL", "Accessories", 30, 14, 20, 4, List.of("S-002", "S-005")),
        new C("Switch Sample Pack", "Keyboards", 12, 5, 8, 2, List.of("S-003")),
        new C("Studio Headphones", "Audio", 149, 95, 120, 12, List.of("S-004")),
        new C("USB DAC", "Audio", 89, 52, 70, 9, List.of("S-004")),
        new C("Boom Arm", "Audio", 65, 38, 50, 6, List.of("S-004", "S-005")),
        new C("Braided HDMI Cable", "Cables", 18, 7, 12, 2, List.of("S-005")),
        new C("Cable Management Tray", "Cables", 22, 10, 16, 3, List.of("S-005", "S-002")),
        new C("Palm Rest", "Keyboards", 35, 18, 26, 4, List.of("S-001")),
        new C("Numpad Module", "Keyboards", 49, 28, 38, 5, List.of("S-003")),
        new C("Travel Keyboard Bag", "Keyboards", 40, 22, 30, 5, List.of("S-002")));

    for (var item : CATALOG) {
      for (String sellerId : item.sellers()) {
        int idx = -1;
        for (int i = 0; i < SELLERS.size(); i++) if (SELLERS.get(i).id().equals(sellerId)) idx = i;
        double jitter = 1 + (idx - 2) * 0.03;
        ProductEntity p = new ProductEntity();
        p.productId = UUID.randomUUID().toString().replace("-", "");
        p.name = item.name();
        p.price = Math.round(item.price() * jitter);
        p.minPrice = item.min();
        p.gap = item.price() - item.min();
        p.remainings = 10;
        p.category = item.category();
        p.imageUrl = "https://picsum.photos/seed/"
            + item.name().toLowerCase().replaceAll("[^a-z0-9]+", "-") + "/600/400";
        p.sellerId = sellerId;
        products.save(p);
        SellerAiConfigEntity cfg = new SellerAiConfigEntity();
        cfg.agentId = UUID.randomUUID().toString().replace("-", "");
        cfg.productId = p.productId;
        cfg.autoAcceptPrice = item.auto();
        cfg.maxDiscountStep = item.step();
        sellerConfigs.save(cfg);
      }
    }

    var now = java.time.Instant.now();
    var start = now.minus(1, java.time.temporal.ChronoUnit.DAYS);
    var end = now.plus(30, java.time.temporal.ChronoUnit.DAYS);
    discountService.seedDiscount("WELCOME10", "10% off your first deal", 0.10, null, null, null, start, end);
    discountService.seedDiscount("KEYLAB5", "5 off, KeyLab Store", null, 5.0, null, "S-001", start, end);
    discountService.seedDiscount("AUDIO15", "15% off Aurora Audio", 0.15, null, null, "S-004", start, end);

    System.out.printf("seeded: sellers=%d buyers=%d products=%d coupons=3 "
        + "(logins: <firstname>@example.com / password)%n",
        sellers.count(), buyers.count(), products.count());
  }
}
