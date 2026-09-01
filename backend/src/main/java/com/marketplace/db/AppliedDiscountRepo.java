package com.marketplace.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedDiscountRepo
    extends JpaRepository<AppliedDiscountEntity, AppliedDiscountId> {
  List<AppliedDiscountEntity> findByNegotiationId(String negotiationId);
}
