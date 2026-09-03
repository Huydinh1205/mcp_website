package com.marketplace;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-off: give products a real, topical image. thanh's Product.URL is NULL for
 * the whole bulk import, so we point it at LoremFlickr (real Flickr photos by
 * keyword, deterministic via ?lock=<id>). Keyword is guessed from the name.
 *
 * Enable once with app.backfill-images=true (env APP_BACKFILL_IMAGES=true),
 * then turn it off — the URLs are persisted.
 */
@Component
public class ImageBackfillRunner implements CommandLineRunner {

  @PersistenceContext private EntityManager em;

  @Value("${app.backfill-images:false}")
  private boolean enabled;

  private static final String BASE = "https://loremflickr.com/600/700/";

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled) return;

    // name LIKE ... -> keyword. Order matters: first match wins, so run the
    // most specific buckets first and "sleepwear" last as the catch-all.
    record Bucket(String keyword, List<String> likes) {}
    List<Bucket> buckets = List.of(
        new Bucket("bathrobe", List.of("%robe%")),
        new Bucket("nightgown", List.of("%nightgown%", "%nightdress%", "%night dress%")),
        new Bucket("pajamas", List.of("%pajama%", "%pyjama%", "%pj set%")),
        new Bucket("lingerie", List.of("%lingerie%", "%babydoll%", "%chemise%", "%teddy%")),
        new Bucket("loungewear", List.of("%lounge%", "%hooded%", "%blanket%")),
        new Bucket("sleepwear", List.of("%")));   // catch-all

    int total = 0;
    for (Bucket b : buckets) {
      StringBuilder where = new StringBuilder(
          "(p.imageUrl IS NULL OR p.imageUrl LIKE '%picsum.photos%') AND (");
      for (int i = 0; i < b.likes().size(); i++) {
        if (i > 0) where.append(" OR ");
        where.append("LOWER(p.name) LIKE :k").append(i);
      }
      where.append(")");

      var q = em.createQuery(
          "UPDATE ProductEntity p SET p.imageUrl = CONCAT('" + BASE + b.keyword()
              + "/all?lock=', p.id) WHERE " + where);
      for (int i = 0; i < b.likes().size(); i++) q.setParameter("k" + i, b.likes().get(i));
      int n = q.executeUpdate();
      total += n;
      System.out.printf("backfill-images: %-11s <- %d products%n", b.keyword(), n);
    }
    System.out.println("backfill-images: done, " + total + " products updated");
  }
}
