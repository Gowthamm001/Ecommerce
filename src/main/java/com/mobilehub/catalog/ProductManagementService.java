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
        String category = defaultIfBlank(request.category(), "Mobiles");
        String ram = defaultIfBlank(request.ram(), "N/A");
        String storage = defaultIfBlank(request.storage(), "N/A");
        String display = defaultIfBlank(request.display(), "N/A");
        String processor = defaultIfBlank(request.processor(), "N/A");
        String camera = defaultIfBlank(request.camera(), "N/A");
        String battery = defaultIfBlank(request.battery(), "N/A");

        Product product = new Product(request.id(), request.brand(), request.model(), request.price(),
                ram, storage, display, processor, camera, battery, request.rating(), request.year(), request.image(),
                blankToNull(request.fallbackImage()) == null ? request.image() : request.fallbackImage(),
                category);
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
        String category = (request.category() == null || request.category().isBlank())
                ? product.getCategory() : request.category().trim();
        String ram = defaultIfBlank(request.ram(), product.getRam());
        String storage = defaultIfBlank(request.storage(), product.getStorage());
        String display = defaultIfBlank(request.display(), product.getDisplay());
        String processor = defaultIfBlank(request.processor(), product.getProcessor());
        String camera = defaultIfBlank(request.camera(), product.getCamera());
        String battery = defaultIfBlank(request.battery(), product.getBattery());

        product.update(request.brand(), request.model(), request.price(), ram, storage,
                display, processor, camera, battery,
                request.rating(), request.year(), request.image(),
                blankToNull(request.fallbackImage()) == null ? request.image() : request.fallbackImage(),
                category);
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

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public record ProductRequest(
            @NotBlank @Size(max = 255) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*")
            String id,
            @NotBlank @Size(max = 255) String brand,
            @NotBlank @Size(max = 255) String model,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @Size(max = 255) String ram,
            @Size(max = 255) String storage,
            @Size(max = 300) String display,
            @Size(max = 200) String processor,
            @Size(max = 300) String camera,
            @Size(max = 255) String battery,
            @Min(0) @Max(5) double rating,
            @Min(2000) @Max(2100) int year,
            @NotBlank @Size(max = 1000) String image,
            @Size(max = 1000) String fallbackImage,
            @Min(0) int stock,
            @Size(max = 100) String category) { }
}
