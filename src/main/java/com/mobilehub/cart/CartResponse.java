package com.mobilehub.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<Item> items,
        int itemCount,
        BigDecimal subtotal,
        BigDecimal delivery,
        BigDecimal total,
        String deliveryNote) {

    public record Item(Long id, String productId, String name, BigDecimal price,
                       String image, String fallback, int quantity) { }
}
