package com.marketplace.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepo extends JpaRepository<ProductEntity, String> {
  List<ProductEntity> findByNameContainingIgnoreCase(String q);

  List<ProductEntity> findBySellerId(String sellerId);
}
