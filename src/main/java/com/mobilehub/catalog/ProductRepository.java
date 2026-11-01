package com.mobilehub.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByIdContainingIgnoreCaseOrBrandContainingIgnoreCaseOrModelContainingIgnoreCase(
            String id, String brand, String model);
}
