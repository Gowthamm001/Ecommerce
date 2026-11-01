package com.mobilehub.catalog;

import com.mobilehub.cart.CartItemRepository;
import com.mobilehub.orders.PurchaseOrderItemRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
public class ProductManagementService {
    private final ProductRepository products;
    private final ProductInventoryRepository inventory;
    private final CartItemRepository cartItems;
    private final PurchaseOrderItemRepository orderItems;
    private final ProductCatalogService catalog;

    public ProductManagementService(ProductRepository products, ProductInventoryRepository inventory,
                                    CartItemRepository cartItems, PurchaseOrderItemRepository orderItems,
                                    ProductCatalogService catalog) {
        this.products = products;
        this.inventory = inventory;
        this.cartItems = cartItems;
        this.orderItems = orderItems;
        this.catalog = catalog;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (products.existsById(request.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with this ID already exists.");
        }
        Product product = new Product(request.id(), request.brand(), request.model(), request.price(),
                request.ram(), request.storage(), request.display(), request.processor(), request.camera(),
                request.battery(), request.rating(), request.year(), request.image(),
                blankToNull(request.fallbackImage()) == null ? request.image() : request.fallbackImage());
        product = products.save(product);
        inventory.save(new ProductInventory(product, request.stock()));
        return catalog.findById(product.getId());
    }

    @Transactional
    public ProductResponse update(String id, ProductRequest request) {
        if (!id.equals(request.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID in the body must match the URL.");
        }
        Product product = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        product.update(request.brand(), request.model(), request.price(), request.ram(), request.storage(),
                request.display(), request.processor(), request.camera(), request.battery(),
                request.rating(), request.year(), request.image(),
                blankToNull(request.fallbackImage()) == null ? request.image() : request.fallbackImage());
        products.save(product);
        ProductInventory stock = inventory.lockByProductId(id)
                .orElseGet(() -> new ProductInventory(product, 0));
        stock.setStock(request.stock());
        inventory.save(stock);
        return catalog.findById(id);
    }

    @Transactional
    public void delete(String id) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        if (cartItems.existsByProduct_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Remove this product from customer carts before deleting it.");
        }
        orderItems.detachProductReference(id);
        inventory.deleteById(id);
        products.delete(product);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record ProductRequest(
            @NotBlank @Size(max = 255) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*")
            String id,
            @NotBlank @Size(max = 255) String brand,
            @NotBlank @Size(max = 255) String model,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotBlank @Size(max = 255) String ram,
            @NotBlank @Size(max = 255) String storage,
            @NotBlank @Size(max = 300) String display,
            @NotBlank @Size(max = 200) String processor,
            @NotBlank @Size(max = 300) String camera,
            @NotBlank @Size(max = 255) String battery,
            @Min(0) @Max(5) double rating,
            @Min(2000) @Max(2100) int year,
            @NotBlank @Size(max = 1000) String image,
            @Size(max = 1000) String fallbackImage,
            @Min(0) int stock) { }
}
