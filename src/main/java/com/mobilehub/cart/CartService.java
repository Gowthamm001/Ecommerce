package com.mobilehub.cart;

import com.mobilehub.account.Account;
import com.mobilehub.catalog.Product;
import com.mobilehub.catalog.ProductInventory;
import com.mobilehub.catalog.ProductInventoryRepository;
import com.mobilehub.catalog.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {
    private static final List<String> COLORS = List.of("Midnight", "Silver", "Blue");

    private final CartItemRepository cartItems;
    private final ProductRepository products;
    private final ProductInventoryRepository inventory;

    public CartService(CartItemRepository cartItems, ProductRepository products,
                       ProductInventoryRepository inventory) {
        this.cartItems = cartItems;
        this.products = products;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Account account) {
        return response(cartItems.findByAccountIdOrderByIdAsc(account.getId()));
    }

    @Transactional
    public CartResponse add(Account account, AddItemRequest request) {
        Product product = products.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        ProductInventory stock = inventory.lockByProductId(product.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inventory has not been initialized for this product."));
        String ram = request.variantRam() == null ? product.getRam() : request.variantRam();
        String storage = request.variantStorage() == null ? product.getStorage() : request.variantStorage();
        String color = request.color() == null ? COLORS.get(0) : request.color();
        BigDecimal price = configurationPrice(product, ram, storage, color);
        int currentQuantity = cartItems.findByAccountIdOrderByIdAsc(account.getId()).stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .mapToInt(CartItem::getQuantity).sum();
        if (currentQuantity >= stock.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requested quantity exceeds available stock.");
        }
        CartItem item = cartItems.findByAccountAndProduct_IdAndVariantRamAndVariantStorageAndColor(
                account, product.getId(), ram, storage, color).orElse(null);
        if (item == null) {
            if (stock.getStock() < 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This product is currently out of stock.");
            }
            cartItems.save(new CartItem(account, product, ram, storage, color, price));
        } else if (item.getQuantity() >= stock.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requested quantity exceeds available stock.");
        } else if (item.getQuantity() >= 99) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart quantity cannot exceed 99.");
        } else {
            item.setQuantity(item.getQuantity() + 1);
        }
        return getCart(account);
    }

    @Transactional
    public CartResponse setQuantity(Account account, Long itemId, int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be between 1 and 99.");
        }
        CartItem item = cartItems.findByIdAndAccount(itemId, account)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found."));
        ProductInventory stock = inventory.lockByProductId(item.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inventory has not been initialized for this product."));
        int otherQuantity = cartItems.findByAccountIdOrderByIdAsc(account.getId()).stream()
                .filter(entry -> entry.getProduct().getId().equals(item.getProduct().getId())
                        && !entry.getId().equals(itemId))
                .mapToInt(CartItem::getQuantity).sum();
        if (quantity + otherQuantity > stock.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requested quantity exceeds available stock.");
        }
        item.setQuantity(quantity);
        return getCart(account);
    }

    @Transactional
    public CartResponse mergeGuestCart(Account account, List<GuestCartItemRequest> requests) {
        if (requests == null || requests.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest cart contains an invalid number of items.");
        }
        Map<CartKey, MergeEntry> additions = new LinkedHashMap<>();
        for (GuestCartItemRequest request : requests) {
            if (request.quantity() < 1 || request.quantity() > 99) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart quantity must be between 1 and 99.");
            }
            Product product = products.findById(request.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
            String ram = request.variantRam() == null || request.variantRam().isBlank()
                    ? product.getRam() : request.variantRam();
            String storage = request.variantStorage() == null || request.variantStorage().isBlank()
                    ? product.getStorage() : request.variantStorage();
            String color = request.color() == null || request.color().isBlank()
                    ? COLORS.get(0) : request.color();
            BigDecimal price = configurationPrice(product, ram, storage, color);
            CartKey key = new CartKey(product.getId(), ram, storage, color);
            MergeEntry current = additions.get(key);
            int quantity = request.quantity() + (current == null ? 0 : current.quantity());
            if (quantity > 99) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart quantity cannot exceed 99.");
            }
            additions.put(key, new MergeEntry(product, ram, storage, color, price, quantity));
        }

        Map<String, ProductInventory> lockedStock = new HashMap<>();
        additions.keySet().stream().map(CartKey::productId).distinct().sorted().forEach(productId ->
                lockedStock.put(productId, inventory.lockByProductId(productId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                "Inventory is unavailable for a product in your cart."))));

        List<CartItem> existing = cartItems.findByAccountIdOrderByIdAsc(account.getId());
        Map<CartKey, CartItem> existingByKey = new HashMap<>();
        existing.forEach(item -> {
            existingByKey.put(new CartKey(item.getProduct().getId(), item.getVariantRam(),
                    item.getVariantStorage(), item.getColor()), item);
        });
        additions.forEach((key, addition) -> {
            CartItem prior = existingByKey.get(key);
            if (prior != null && prior.getQuantity() + addition.quantity() > 99) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart quantity cannot exceed 99.");
            }
        });

        additions.keySet().stream().map(CartKey::productId).distinct().sorted().forEach(productId -> {
            int currentQuantity = existing.stream()
                    .filter(item -> item.getProduct().getId().equals(productId))
                    .mapToInt(CartItem::getQuantity).sum();
            int addedQuantity = additions.entrySet().stream()
                    .filter(entry -> entry.getKey().productId().equals(productId))
                    .mapToInt(entry -> entry.getValue().quantity()).sum();
            if (currentQuantity + addedQuantity > lockedStock.get(productId).getStock()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "The guest cart exceeds available stock for " + additions.values().stream()
                                .filter(entry -> entry.product().getId().equals(productId))
                                .findFirst().orElseThrow().product().getModel() + ".");
            }
        });

        additions.forEach((key, addition) -> {
            CartItem prior = existingByKey.get(key);
            if (prior != null) {
                prior.setQuantity(prior.getQuantity() + addition.quantity());
            } else {
                CartItem created = new CartItem(account, addition.product(), addition.ram(),
                        addition.storage(), addition.color(), addition.price());
                created.setQuantity(addition.quantity());
                cartItems.save(created);
            }
        });
        return getCart(account);
    }

    private BigDecimal configurationPrice(Product product, String ram, String storage, String color) {
        String pRam = product.getRam() == null ? "" : product.getRam().trim();
        String pStorage = product.getStorage() == null ? "" : product.getStorage().trim();

        if (pRam.matches("^\\d+\\s*GB.*")) {
            int ramNumber = Integer.parseInt(pRam.split("\\s+")[0]);
            String secondRam = ramNumber < 12 ? "12 GB" : (ramNumber < 16 ? "16 GB" : (ramNumber < 32 ? "32 GB" : "64 GB"));
            String secondStorage = "512 GB".equalsIgnoreCase(pStorage) ? "1 TB"
                    : ("1 TB".equalsIgnoreCase(pStorage) ? "2 TB" : "512 GB");

            boolean baseVariant = (ram == null || ram.isBlank() || ram.equalsIgnoreCase(pRam) || "Standard".equalsIgnoreCase(ram))
                    && (storage == null || storage.isBlank() || storage.equalsIgnoreCase(pStorage) || "Standard".equalsIgnoreCase(storage));
            boolean upgradedVariant = secondRam.equalsIgnoreCase(ram) && secondStorage.equalsIgnoreCase(storage);

            if (upgradedVariant) {
                return product.getPrice().add(BigDecimal.valueOf(7000));
            }
            if (baseVariant) {
                return product.getPrice();
            }
        }
        return product.getPrice();
    }

    private record CartKey(String productId, String ram, String storage, String color) { }
    private record MergeEntry(Product product, String ram, String storage, String color,
                              BigDecimal price, int quantity) { }

    public record GuestCartItemRequest(
            @jakarta.validation.constraints.NotBlank String productId,
            String variantRam,
            String variantStorage,
            String color,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(99) int quantity) { }

    @Transactional
    public CartResponse remove(Account account, Long itemId) {
        CartItem item = cartItems.findByIdAndAccount(itemId, account)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found."));
        cartItems.delete(item);
        return getCart(account);
    }

    private CartResponse response(List<CartItem> items) {
        List<CartResponse.Item> rows = items.stream().map(item -> {
            String name;
            String vRam = item.getVariantRam();
            String vStorage = item.getVariantStorage();
            boolean hasSpecs = vRam != null && !vRam.isBlank() && !"N/A".equalsIgnoreCase(vRam) && !"Standard".equalsIgnoreCase(vRam);
            if (hasSpecs) {
                name = item.getProduct().getBrand() + " " + item.getProduct().getModel() + " · "
                        + vRam + " / " + vStorage + " · " + item.getColor();
            } else if (item.getColor() != null && !item.getColor().isBlank() && !"Standard".equalsIgnoreCase(item.getColor())) {
                name = item.getProduct().getBrand() + " " + item.getProduct().getModel() + " · " + item.getColor();
            } else {
                name = item.getProduct().getBrand() + " " + item.getProduct().getModel();
            }
            return new CartResponse.Item(
                    item.getId(),
                    item.getProduct().getId(),
                    name,
                    item.getUnitPrice(),
                    item.getProduct().getImage(),
                    item.getProduct().getFallbackImage(),
                    item.getQuantity());
        }).toList();
        int count = items.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal subtotal = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal delivery = subtotal.signum() == 0 || subtotal.compareTo(BigDecimal.valueOf(10000)) >= 0
                ? BigDecimal.ZERO : BigDecimal.valueOf(499);
        String note = subtotal.signum() == 0
                ? "Shipping is calculated at checkout."
                : delivery.signum() == 0
                    ? "Your order qualifies for free delivery."
                    : "Add ₹" + String.format(java.util.Locale.forLanguageTag("en-IN"), "%,.0f",
                            BigDecimal.valueOf(10000).subtract(subtotal)) + " more for free delivery.";
        return new CartResponse(rows, count, subtotal, delivery, subtotal.add(delivery), note);
    }

    public record AddItemRequest(
            @jakarta.validation.constraints.NotBlank String productId,
            String variantRam,
            String variantStorage,
            String color) { }
    public record QuantityRequest(@jakarta.validation.constraints.Min(1)
                                  @jakarta.validation.constraints.Max(99) int quantity) { }
}
