package com.marketplace;

import com.marketplace.db.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-off: give the top-N products a handful of WRITTEN reviews so the review
 * list isn't empty under the imported aggregate rating. Reviews are invoice-
 * backed ([Feedback] PK is Product_ID + Invoice_ID): we make 5 synthetic
 * Order+Invoice rows owned by real buyers (varied reviewer names), then one
 * set-based INSERT..SELECT hangs 5 Feedback rows off each product that has none.
 *
 * Enable once with app.backfill-reviews=true (env APP_BACKFILL_REVIEWS=true);
 * app.backfill-reviews-limit caps the product count (default 400).
 */
@Component
public class ReviewBackfillRunner implements CommandLineRunner {

  @PersistenceContext private EntityManager em;
  @Value("${app.backfill-reviews:false}") private boolean enabled;
  @Value("${app.backfill-reviews-limit:400}") private int productLimit;

  private final BuyerRepo buyers;
  private final OrderRepo orders;
  private final InvoiceRepo invoices;

  public ReviewBackfillRunner(BuyerRepo buyers, OrderRepo orders, InvoiceRepo invoices) {
    this.buyers = buyers;
    this.orders = orders;
    this.invoices = invoices;
  }

  private static final String[] CMT = {
    "Exactly as pictured, super soft and warm.",
    "Great fit, washed well, would buy again.",
    "Cosy and good value for the price.",
    "Nicer material than I expected. Fast shipping.",
    "Comfortable but the sizing runs a little large.",
  };
  private static final int[] SCORE = {5, 4, 5, 4, 3};

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled) return;

    var buyerPage = buyers.findAll(PageRequest.of(0, 30));
    List<Long> buyerIds = new ArrayList<>();
    buyerPage.forEach(b -> buyerIds.add(b.id));
    if (buyerIds.isEmpty()) { System.out.println("backfill-reviews: no buyers, skipped"); return; }

    Random rnd = new Random(7);
    long[] inv = new long[5];
    for (int i = 0; i < 5; i++) {
      OrderEntity o = new OrderEntity();
      o.status = "PAID";
      orders.save(o);
      InvoiceEntity v = new InvoiceEntity();
      v.orderId = o.id;
      v.buyerId = buyerIds.get(rnd.nextInt(buyerIds.size()));
      v.paymentId = 1;
      v.quantity = 1;
      v.totalCost = 0;
      inv[i] = invoices.save(v).id;
    }
    em.flush();

    int lim = Math.max(1, productLimit);
    String sql =
        "INSERT INTO dbo.Feedback (Product_ID, Invoice_ID, Rating_Score, Comment, Created_At) "
      + "SELECT p.Product_ID, v.inv, v.score, v.cmt, "
      + "  DATEADD(day, -1 - (ABS(p.Product_ID * 7 + v.inv * 13) % 120), SYSUTCDATETIME()) "
      + "FROM (SELECT TOP (" + lim + ") Product_ID FROM dbo.Product ORDER BY Product_ID) p "
      + "CROSS JOIN (VALUES (?,?,?),(?,?,?),(?,?,?),(?,?,?),(?,?,?)) v(inv, score, cmt) "
      + "WHERE NOT EXISTS (SELECT 1 FROM dbo.Feedback f WHERE f.Product_ID = p.Product_ID)";
    int n = em.createNativeQuery(sql)
        .setParameter(1, inv[0]).setParameter(2, SCORE[0]).setParameter(3, CMT[0])
        .setParameter(4, inv[1]).setParameter(5, SCORE[1]).setParameter(6, CMT[1])
        .setParameter(7, inv[2]).setParameter(8, SCORE[2]).setParameter(9, CMT[2])
        .setParameter(10, inv[3]).setParameter(11, SCORE[3]).setParameter(12, CMT[3])
        .setParameter(13, inv[4]).setParameter(14, SCORE[4]).setParameter(15, CMT[4])
        .executeUpdate();

    System.out.printf("backfill-reviews: inserted %d Feedback rows (5 synthetic invoices, top %d products)%n",
        n, lim);
  }
}
