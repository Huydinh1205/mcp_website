package com.marketplace.web;

import com.marketplace.FeedService;
import com.marketplace.auth.CurrentUser;
import com.marketplace.negotiation.NegotiationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NegotiationsController {

  private final NegotiationService negotiations;
  private final FeedService feed;

  public NegotiationsController(NegotiationService negotiations, FeedService feed) {
    this.negotiations = negotiations;
    this.feed = feed;
  }

  /** Buyers see their own negotiations; sellers see negotiations for their products. */
  @GetMapping("/api/negotiations")
  public FeedService.FeedResult get(@RequestParam(defaultValue = "") String since) {
    String buyer = CurrentUser.isBuyer() ? CurrentUser.id() : null;
    String seller = CurrentUser.isSeller() ? CurrentUser.id() : null;
    return feed.compute(negotiations.feedRows(buyer, seller), since);
  }
}
