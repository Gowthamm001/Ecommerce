package com.mobilehub.catalog;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, String> {
    List<ProductInventory> findByProductIdIn(Collection<String> productIds);
    Optional<ProductInventory> findByProduct_Id(String productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from ProductInventory inventory where inventory.product.id = :productId")
    Optional<ProductInventory> lockByProductId(@Param("productId") String productId);
}
