package com.marketplace;

import java.util.List;
import org.springframework.stereotype.Service;

/** Pure delta logic for GET /api/negotiations?since=. Port of lib/feed.ts. */
@Service
public class FeedService {

  public record FeedRound(int roundNumber, String author, double proposedPrice, String message) {}

  public record FeedRow(
      String negotiationId,
      String productId,
      String name,
      String status,
      String lastActor,
      int currentRound,
      double currentPrice,
      long updatedAt,
      List<FeedRound> rounds) {}

  public record FeedNegotiation(
      String negotiationId,
      String productId,
      String name,
      String status,
      String lastActor,
      int currentRound,
      double currentPrice,
      long updatedAt,
      List<FeedRound> historyTail) {}

  public record FeedResult(String cursor, List<FeedNegotiation> negotiations) {}

  public FeedResult compute(List<FeedRow> rows, String since) {
    long cutoff;
    boolean hasCutoff;
    try {
      cutoff = (since == null || since.isEmpty()) ? Long.MIN_VALUE : Long.parseLong(since);
      hasCutoff = since != null && !since.isEmpty();
    } catch (NumberFormatException e) {
      cutoff = Long.MIN_VALUE;
      hasCutoff = false;
    }

    final long fCutoff = cutoff;
    List<FeedNegotiation> changed = rows.stream()
        .filter(r -> r.updatedAt() > fCutoff)
        .map(r -> new FeedNegotiation(
            r.negotiationId(), r.productId(), r.name(), r.status(), r.lastActor(),
            r.currentRound(), r.currentPrice(), r.updatedAt(), r.rounds()))
        .toList();

    long max = rows.stream().mapToLong(FeedRow::updatedAt).max().orElse(Long.MIN_VALUE);
    String cursor = (max == Long.MIN_VALUE)
        ? (hasCutoff ? String.valueOf(fCutoff) : "")
        : String.valueOf(max);

    return new FeedResult(cursor, changed);
  }
}
