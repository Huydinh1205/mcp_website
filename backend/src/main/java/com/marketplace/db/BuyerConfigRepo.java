package com.marketplace.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerConfigRepo extends JpaRepository<BuyerAiConfigEntity, String> {
  Optional<BuyerAiConfigEntity> findByNationalId(String nationalId);
}
