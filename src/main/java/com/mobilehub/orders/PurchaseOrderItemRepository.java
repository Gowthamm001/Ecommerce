package com.mobilehub.orders;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    List<PurchaseOrderItem> findByOrder_IdInOrderByIdAsc(Collection<Long> orderIds);

    @Modifying
    @Query("update PurchaseOrderItem item set item.product = null where item.product.id = :productId")
    int detachProductReference(@Param("productId") String productId);
}
