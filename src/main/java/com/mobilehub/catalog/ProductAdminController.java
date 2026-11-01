package com.mobilehub.catalog;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class ProductAdminController {
    private final ProductCatalogService catalog;
    private final ProductManagementService management;

    public ProductAdminController(ProductCatalogService catalog, ProductManagementService management) {
        this.catalog = catalog;
        this.management = management;
    }

    @GetMapping
    public List<ProductResponse> products(@RequestParam(required = false) String q) {
        return catalog.findAll(q);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductManagementService.ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(management.create(request));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable String id,
                                  @Valid @RequestBody ProductManagementService.ProductRequest request) {
        return management.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        management.delete(id);
        return ResponseEntity.noContent().build();
    }
}
