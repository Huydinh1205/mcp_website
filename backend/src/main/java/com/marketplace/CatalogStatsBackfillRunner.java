package com.marketplace;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-off: thanh's bulk import left Rating_Avg / Rating_Count / Sold_Count /
 * Compare_At_Price at 0 for ~95k products, so cards render "0.0 ★ · 0 sold" and
 * no discount. Fill them with plausible, deterministic values (by Product_ID)
 * in one bulk UPDATE. Enable once with app.backfill-stats=true.
 */
@Component
public class CatalogStatsBackfillRunner implements CommandLineRunner {

  @PersistenceContext private EntityManager em;
  @Value("${app.backfill-stats:false}") private boolean enabled;

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled) return;
    int n = em.createQuery("""
        UPDATE ProductEntity p SET
          p.ratingAvg      = 3.8 + MOD(p.id, 12) * 0.1,
          p.ratingCount    = 20 + MOD(p.id, 480) * 4,
          p.soldCount      = (20 + MOD(p.id, 480) * 4) * (4 + MOD(p.id, 9)),
          p.compareAtPrice = CAST(p.price * (1.16 + MOD(p.id, 38) * 0.01) AS double)
        WHERE p.ratingCount = 0
        """).executeUpdate();
    System.out.println("backfill-stats: " + n + " products filled with rating/sold/compare-at");
  }
}
