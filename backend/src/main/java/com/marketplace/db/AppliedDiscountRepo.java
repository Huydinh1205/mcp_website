package com.marketplace.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedDiscountRepo
    extends JpaRepository<AppliedDiscountEntity, AppliedDiscountEntity.Key> {
  List<AppliedDiscountEntity> findByNegotiationId(Long negotiationId);
}
