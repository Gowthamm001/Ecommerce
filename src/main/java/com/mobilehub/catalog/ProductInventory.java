package com.mobilehub.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Check;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "product_inventory")
@Check(constraints = "stock >= 0")
public class ProductInventory implements Persistable<String> {
    @Id
    @Column(name = "product_id", length = 255)
    private String productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int stock;

    @Transient
    private boolean newEntity = true;

    protected ProductInventory() {
    }

    public ProductInventory(Product product, int stock) {
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        this.product = product;
        this.productId = product.getId();
        this.stock = stock;
    }

    public String getProductId() { return productId; }
    public Product getProduct() { return product; }
    public int getStock() { return stock; }

    @Override
    public String getId() { return productId; }

    @Override
    @Transient
    public boolean isNew() { return newEntity; }

    @PostPersist
    @PostLoad
    void markNotNew() { newEntity = false; }

    public void setStock(int stock) {
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        this.stock = stock;
    }
}
