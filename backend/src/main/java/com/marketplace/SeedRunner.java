package com.marketplace;

import com.marketplace.db.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared DB already holds ~95k products / 100 buyers / 31 sellers. We only add:
 *  - 3 demo login accounts (thanh's users have unknown password hashes)
 *  - a "demo shop" that owns a handful of products, dressed with storefront
 *    fields (category / image / ratings) so the catalog looks real
 *  - a couple of coupons in [App_Coupon]
 * Runs once (guarded by the demo buyer's presence).
 */
@Component
public class SeedRunner implements CommandLineRunner {

  private final UserRepo users;
  private final BuyerRepo buyers;
  private final SellerRepo sellers;
  private final BuyerConfigRepo buyerConfigs;
  private final ProductRepo products;
  private final DiscountRepo coupons;
  private final BCryptPasswordEncoder encoder;

  public SeedRunner(UserRepo users, BuyerRepo buyers, SellerRepo sellers,
      BuyerConfigRepo buyerConfigs, ProductRepo products, DiscountRepo coupons,
      BCryptPasswordEncoder encoder) {
    this.users = users;
    this.buyers = buyers;
    this.sellers = sellers;
    this.buyerConfigs = buyerConfigs;
    this.products = products;
    this.coupons = coupons;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (users.findByEmailIgnoreCase("mai.demo@example.com").isPresent()) return;

    String pw = encoder.encode("password");
    Long maiId = newBuyer("Mai", "Tran", "mai.demo@example.com", pw, "mechanical keyboards");
    Long longId = newBuyer("Long", "Pham", "long.demo@example.com", pw, "audio");
    Long shopId = newSeller("KeyLab", "Store", "keylab.demo@example.com", pw);

    // dress up a demo shopfront: reassign the first products to the demo seller
    String[] cats = {"Sleepwear", "Loungewear", "Robes", "Pajamas"};
    var page = products.findByNameContainingIgnoreCase("", PageRequest.of(0, 12));
    int i = 0;
    for (ProductEntity p : page) {
      p.sellerId = shopId;
      p.category = cats[i % cats.length];
      p.imageUrl = "https://picsum.photos/seed/keylab" + p.id + "/500/600";
      if (p.compareAtPrice == null || p.compareAtPrice <= p.price) {
        p.compareAtPrice = Math.round(p.price * 1.35 * 100.0) / 100.0;
      }
      if (p.ratingAvg <= 0) p.ratingAvg = 4.2 + (i % 6) * 0.12;
      if (p.ratingCount <= 0) p.ratingCount = 40 + i * 17;
      if (p.soldCount <= 0) p.soldCount = p.ratingCount * 6;
      products.save(p);
      i++;
    }

    Instant now = Instant.now();
    Instant end = now.plus(365, ChronoUnit.DAYS);
    coupon("WELCOME10", "10% off your first deal", 0.10, null, null, null, now, end);
    coupon("KEYLAB5", "$5 off KeyLab Store", null, 5.0, null, shopId, now, end);
    coupon("DEAL15", "15% off (demo shop)", 0.15, null, null, shopId, now, end);

    System.out.printf("seeded: demo buyers=%d,%d demo shop=%d, %d products dressed, 3 coupons%n",
        maiId, longId, shopId, i);
  }

  private Long newBuyer(String first, String last, String email, String pw, String interest) {
    UserEntity u = new UserEntity();
    u.firstName = first; u.lastName = last; u.email = email; u.passwordHash = pw; u.role = "buyer";
    users.save(u);
    BuyerEntity b = new BuyerEntity();
    b.id = u.id; b.interest = interest;
    buyers.save(b);
    BuyerAiConfigEntity cfg = new BuyerAiConfigEntity();
    cfg.buyerId = u.id; cfg.maxBudget = 200; cfg.targetPrice = 120; cfg.minSellerRating = 0;
    cfg.style = "MODERATE";
    buyerConfigs.save(cfg);
    return u.id;
  }

  private Long newSeller(String first, String last, String email, String pw) {
    UserEntity u = new UserEntity();
    u.firstName = first; u.lastName = last; u.email = email; u.passwordHash = pw; u.role = "seller";
    users.save(u);
    SellerEntity s = new SellerEntity();
    s.id = u.id; s.rating = 4.7; s.totalRatings = 120; s.tradingName = "KeyLab Store";
    sellers.save(s);
    return u.id;
  }

  private void coupon(String code, String label, Double pct, Double amt,
      Long productId, Long sellerId, Instant start, Instant end) {
    DiscountEntity d = new DiscountEntity();
    d.code = code; d.label = label; d.percent = pct; d.amount = amt;
    d.productId = productId; d.sellerId = sellerId; d.startDate = start; d.endDate = end;
    coupons.save(d);
  }
}
