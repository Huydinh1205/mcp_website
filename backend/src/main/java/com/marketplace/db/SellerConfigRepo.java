package com.marketplace.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerConfigRepo extends JpaRepository<SellerAiConfigEntity, String> {
  Optional<SellerAiConfigEntity> findByProductId(String productId);
}
