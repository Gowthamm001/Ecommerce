package com.mobilehub.cart;

import com.mobilehub.account.Account;
import com.mobilehub.catalog.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String variantRam;
    private String variantStorage;
    private String color;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    private int quantity;

    protected CartItem() {
    }

    public CartItem(Account account, Product product, String variantRam, String variantStorage,
                    String color, BigDecimal unitPrice) {
        this.account = account;
        this.product = product;
        this.variantRam = variantRam;
        this.variantStorage = variantStorage;
        this.color = color;
        this.unitPrice = unitPrice;
        this.quantity = 1;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public Product getProduct() { return product; }
    public String getVariantRam() { return variantRam; }
    public String getVariantStorage() { return variantStorage; }
    public String getColor() { return color; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
