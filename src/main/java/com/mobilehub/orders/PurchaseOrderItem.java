package com.mobilehub.orders;

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
@Table(name = "order_items")
public class PurchaseOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name_snapshot", nullable = false, length = 600)
    private String productNameSnapshot;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "variant_ram")
    private String variantRam;

    @Column(name = "variant_storage")
    private String variantStorage;

    private String color;

    @Column(nullable = false)
    private int quantity;

    protected PurchaseOrderItem() {
    }

    public PurchaseOrderItem(PurchaseOrder order, Product product, String productNameSnapshot,
                             BigDecimal unitPrice, String variantRam, String variantStorage,
                             String color, int quantity) {
        this.order = order;
        this.product = product;
        this.productNameSnapshot = productNameSnapshot;
        this.unitPrice = unitPrice;
        this.variantRam = variantRam;
        this.variantStorage = variantStorage;
        this.color = color;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public PurchaseOrder getOrder() { return order; }
    public Product getProduct() { return product; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getVariantRam() { return variantRam; }
    public String getVariantStorage() { return variantStorage; }
    public String getColor() { return color; }
    public int getQuantity() { return quantity; }
}
