package com.marketplace.web;

import com.marketplace.MarketplaceReads;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DirectoryController {

  private final MarketplaceReads reads;

  public DirectoryController(MarketplaceReads reads) {
    this.reads = reads;
  }

  @GetMapping("/api/buyers")
  public List<Map<String, Object>> buyers() {
    return reads.buyersDirectory();
  }

  @GetMapping("/api/sellers")
  public List<Map<String, Object>> sellers() {
    return reads.sellersDirectory();
  }
}
