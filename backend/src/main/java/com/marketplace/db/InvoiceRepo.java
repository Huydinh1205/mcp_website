package com.marketplace.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepo extends JpaRepository<InvoiceEntity, Long> {
  List<InvoiceEntity> findByOrderId(Long orderId);
}
