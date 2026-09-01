package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.db.DiscountEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

/** Pure coupon math + validity. */
class DiscountServiceTest {

  private final DiscountService svc = new DiscountService(null, null, null);

  private DiscountEntity d(Double percent, Double amount) {
    DiscountEntity x = new DiscountEntity();
    x.percent = percent;
    x.amount = amount;
    x.startDate = Instant.now().minus(1, ChronoUnit.DAYS);
    x.endDate = Instant.now().plus(1, ChronoUnit.DAYS);
    return x;
  }

  @Test
  void percentDiscount() {
    assertThat(svc.discountAmount(100, d(0.2, null))).isEqualTo(20.0);
  }

  @Test
  void flatAmountDiscount() {
    assertThat(svc.discountAmount(100, d(null, 15.0))).isEqualTo(15.0);
  }

  @Test
  void discountNeverExceedsBasePrice() {
    assertThat(svc.discountAmount(10, d(null, 15.0))).isEqualTo(10.0);
    assertThat(svc.discountAmount(10, d(2.0, null))).isEqualTo(10.0);
  }

  @Test
  void activeOnlyInsideTheDateWindow() {
    DiscountEntity now = d(0.1, null);
    assertThat(svc.isActive(now, Instant.now())).isTrue();

    DiscountEntity past = d(0.1, null);
    past.endDate = Instant.now().minus(1, ChronoUnit.HOURS);
    assertThat(svc.isActive(past, Instant.now())).isFalse();

    DiscountEntity future = d(0.1, null);
    future.startDate = Instant.now().plus(1, ChronoUnit.HOURS);
    assertThat(svc.isActive(future, Instant.now())).isFalse();
  }

  @Test
  void appliesTo_productSellerOrGlobal() {
    DiscountEntity forProduct = d(0.1, null);
    forProduct.productId = "P1";
    assertThat(svc.appliesTo(forProduct, "P1", "S1")).isTrue();
    assertThat(svc.appliesTo(forProduct, "P2", "S1")).isFalse();

    DiscountEntity forSeller = d(0.1, null);
    forSeller.sellerId = "S1";
    assertThat(svc.appliesTo(forSeller, "Pany", "S1")).isTrue();
    assertThat(svc.appliesTo(forSeller, "Pany", "S2")).isFalse();

    DiscountEntity global = d(0.1, null);
    assertThat(svc.appliesTo(global, "Pany", "Sany")).isTrue();
  }
}
