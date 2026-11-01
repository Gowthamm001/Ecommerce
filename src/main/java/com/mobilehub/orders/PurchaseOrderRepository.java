package com.mobilehub.orders;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByAccountIdOrderByPlacedAtDesc(String accountId);
}
