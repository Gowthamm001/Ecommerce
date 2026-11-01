package com.mobilehub.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        LocalDateTime placedAt,
        String status,
        BigDecimal subtotal,
        BigDecimal delivery,
        BigDecimal total,
        List<Item> items) {

    public record Item(
            Long id,
            String productName,
            String variantRam,
            String variantStorage,
            String color,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal) { }
}
