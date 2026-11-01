package com.mobilehub.orders;

import com.mobilehub.account.Account;
import com.mobilehub.cart.CartItem;
import com.mobilehub.cart.CartItemRepository;
import com.mobilehub.catalog.ProductInventory;
import com.mobilehub.catalog.ProductInventoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PurchaseOrderService {
    private final CartItemRepository cartItems;
    private final ProductInventoryRepository inventory;
    private final PurchaseOrderRepository orders;
    private final PurchaseOrderItemRepository orderItems;

    public PurchaseOrderService(CartItemRepository cartItems, ProductInventoryRepository inventory,
                                PurchaseOrderRepository orders, PurchaseOrderItemRepository orderItems) {
        this.cartItems = cartItems;
        this.inventory = inventory;
        this.orders = orders;
        this.orderItems = orderItems;
    }

    @Transactional
    public PurchaseOrderResponse placeOrder(Account account) {
        List<CartItem> cart = cartItems.findByAccountIdOrderByIdAsc(account.getId());
        if (cart.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty.");
        }

        Map<String, Integer> requestedByProduct = new HashMap<>();
        cart.forEach(item -> requestedByProduct.merge(
                item.getProduct().getId(), item.getQuantity(), Integer::sum));
        Map<String, ProductInventory> lockedInventory = new HashMap<>();
        requestedByProduct.keySet().stream().sorted().forEach(productId -> {
            ProductInventory stock = inventory.lockByProductId(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Inventory is unavailable for a product in your cart."));
            if (stock.getStock() < requestedByProduct.get(productId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient stock for " + cart.stream()
                                .filter(item -> item.getProduct().getId().equals(productId))
                                .findFirst().orElseThrow().getProduct().getModel() + ".");
            }
            lockedInventory.put(productId, stock);
        });

        BigDecimal subtotal = cart.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal delivery = subtotal.compareTo(BigDecimal.valueOf(10000)) >= 0
                ? BigDecimal.ZERO : BigDecimal.valueOf(499);
        PurchaseOrder order = orders.saveAndFlush(new PurchaseOrder(account, LocalDateTime.now(),
                subtotal, delivery, subtotal.add(delivery)));

        List<PurchaseOrderItem> savedItems = new ArrayList<>();
        for (CartItem cartItem : cart) {
            savedItems.add(new PurchaseOrderItem(order, cartItem.getProduct(),
                    cartItem.getProduct().getBrand() + " " + cartItem.getProduct().getModel(),
                    cartItem.getUnitPrice(), cartItem.getVariantRam(), cartItem.getVariantStorage(),
                    cartItem.getColor(), cartItem.getQuantity()));
        }
        savedItems = orderItems.saveAllAndFlush(savedItems);

        requestedByProduct.forEach((productId, quantity) ->
                lockedInventory.get(productId).setStock(lockedInventory.get(productId).getStock() - quantity));
        cartItems.deleteAll(cart);
        return toResponse(order, savedItems);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> history(Account account) {
        List<PurchaseOrder> customerOrders = orders.findByAccountIdOrderByPlacedAtDesc(account.getId());
        if (customerOrders.isEmpty()) return List.of();
        Map<Long, List<PurchaseOrderItem>> itemsByOrder = new HashMap<>();
        orderItems.findByOrder_IdInOrderByIdAsc(customerOrders.stream().map(PurchaseOrder::getId).toList())
                .forEach(item -> itemsByOrder.computeIfAbsent(item.getOrder().getId(), ignored -> new ArrayList<>())
                        .add(item));
        return customerOrders.stream()
                .map(order -> toResponse(order, itemsByOrder.getOrDefault(order.getId(), List.of())))
                .toList();
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order, List<PurchaseOrderItem> items) {
        List<PurchaseOrderResponse.Item> rows = items.stream().map(item ->
                new PurchaseOrderResponse.Item(item.getId(), item.getProductNameSnapshot(),
                        item.getVariantRam(), item.getVariantStorage(), item.getColor(), item.getUnitPrice(),
                        item.getQuantity(), item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();
        return new PurchaseOrderResponse(order.getId(), order.getPlacedAt(), order.getStatus(),
                order.getSubtotal(), order.getDelivery(), order.getTotal(), rows);
    }
}
