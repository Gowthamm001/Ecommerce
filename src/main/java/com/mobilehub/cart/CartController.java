package com.mobilehub.cart;

import com.mobilehub.account.Account;
import com.mobilehub.cart.CartService.GuestCartItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping
    public CartResponse get(Authentication authentication) {
        return cart.getCart((Account) authentication.getPrincipal());
    }

    @PostMapping("/items")
    public CartResponse add(Authentication authentication, @Valid @RequestBody CartService.AddItemRequest request) {
        return cart.add((Account) authentication.getPrincipal(), request);
    }

    @PostMapping("/merge")
    public CartResponse merge(Authentication authentication, @Valid @RequestBody GuestCartRequest request) {
        return cart.mergeGuestCart((Account) authentication.getPrincipal(), request.items());
    }

    @PutMapping("/items/{id}")
    public CartResponse quantity(Authentication authentication, @PathVariable Long id,
                                 @Valid @RequestBody CartService.QuantityRequest request) {
        return cart.setQuantity((Account) authentication.getPrincipal(), id, request.quantity());
    }

    @DeleteMapping("/items/{id}")
    public CartResponse remove(Authentication authentication, @PathVariable Long id) {
        return cart.remove((Account) authentication.getPrincipal(), id);
    }

    public record GuestCartRequest(
            @jakarta.validation.constraints.NotNull
            @Size(max = 100)
            List<@Valid GuestCartItemRequest> items) { }
}
