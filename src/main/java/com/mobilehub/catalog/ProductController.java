package com.mobilehub.catalog;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductCatalogService catalog;

    public ProductController(ProductCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<ProductResponse> products(@RequestParam(required = false) String q) {
        return catalog.findAll(q);
    }

    @GetMapping("/{id}")
    public ProductResponse product(@PathVariable String id) {
        ProductResponse product = catalog.findById(id);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found.");
        }
        return product;
    }
}
