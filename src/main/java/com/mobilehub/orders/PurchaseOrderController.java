package com.mobilehub.orders;

import com.mobilehub.account.Account;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class PurchaseOrderController {
    private final PurchaseOrderService orders;

    public PurchaseOrderController(PurchaseOrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> placeOrder(Authentication authentication) {
        PurchaseOrderResponse order = orders.placeOrder((Account) authentication.getPrincipal());
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping
    public List<PurchaseOrderResponse> history(Authentication authentication) {
        return orders.history((Account) authentication.getPrincipal());
    }
}
