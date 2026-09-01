package com.marketplace.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountRepo extends JpaRepository<DiscountEntity, String> {
  Optional<DiscountEntity> findByCodeIgnoreCase(String code);
}
