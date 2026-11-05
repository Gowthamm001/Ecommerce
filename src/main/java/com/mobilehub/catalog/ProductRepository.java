package com.mobilehub.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByIdContainingIgnoreCaseOrBrandContainingIgnoreCaseOrModelContainingIgnoreCase(
            String id, String brand, String model);

    List<Product> findByCategoryIgnoreCase(String category);

    @Query("SELECT p FROM Product p WHERE LOWER(p.category) = LOWER(:category) AND (LOWER(p.id) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(p.model) LIKE LOWER(CONCAT('%', :term, '%')))")
    List<Product> searchByCategoryAndTerm(@Param("category") String category, @Param("term") String term);

    @Modifying
    @Query("UPDATE Product p SET p.category = 'Mobiles' WHERE p.category IS NULL OR TRIM(p.category) = ''")
    int backfillNullCategories();
}
