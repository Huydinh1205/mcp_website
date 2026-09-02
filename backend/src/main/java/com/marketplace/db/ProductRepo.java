package com.marketplace.db;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepo extends JpaRepository<ProductEntity, Long> {
  // Catalog has ~95k rows on the shared DB: always page the name search.
  List<ProductEntity> findByNameContainingIgnoreCase(String q, Pageable page);

  List<ProductEntity> findBySellerId(Long sellerId);
}
