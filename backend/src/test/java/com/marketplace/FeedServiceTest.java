package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Port of lib/feed.test.ts */
class FeedServiceTest {

  private final FeedService feed = new FeedService();

  private FeedService.FeedRow row(String id, long updatedAt) {
    return new FeedService.FeedRow(
        id, "P1", "Keyboard", 1, 0.0, false, "countered", "buyer", 1, 45.0, updatedAt,
        List.of(new FeedService.FeedRound(1, "buyer", 45.0, "hi")));
  }

  @Test
  void emptyCursorReturnsEveryRow() {
    var res = feed.compute(List.of(row("a", 100), row("b", 200), row("c", 300)), "");
    assertThat(res.negotiations()).extracting(FeedService.FeedNegotiation::negotiationId)
        .containsExactly("a", "b", "c");
  }

  @Test
  void cursorFiltersToNewerRows() {
    var res = feed.compute(List.of(row("a", 100), row("b", 200), row("c", 300)), "200");
    assertThat(res.negotiations()).extracting(FeedService.FeedNegotiation::negotiationId)
        .containsExactly("c");
  }

  @Test
  void cursorAdvancesToNewest() {
    assertThat(feed.compute(List.of(row("a", 100), row("c", 300)), "").cursor()).isEqualTo("300");
    assertThat(feed.compute(List.of(), "50").cursor()).isEqualTo("50");
  }

  @Test
  void unrecognizedCursorSelfHeals() {
    var res = feed.compute(List.of(row("a", 100), row("b", 200)), "garbage");
    assertThat(res.negotiations()).hasSize(2);
  }

  @Test
  void carriesRoundsAsHistoryTail() {
    var res = feed.compute(List.of(row("a", 100)), "");
    assertThat(res.negotiations().get(0).historyTail()).hasSize(1);
    assertThat(res.negotiations().get(0).historyTail().get(0).proposedPrice()).isEqualTo(45.0);
  }
}
