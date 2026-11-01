package com.mobilehub.orders;

import com.mobilehub.account.Account;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "placed_at", nullable = false)
    private LocalDateTime placedAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal delivery;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 32)
    private String status;

    protected PurchaseOrder() {
    }

    public PurchaseOrder(Account account, LocalDateTime placedAt, BigDecimal subtotal,
                         BigDecimal delivery, BigDecimal total) {
        this.account = account;
        this.placedAt = placedAt;
        this.subtotal = subtotal;
        this.delivery = delivery;
        this.total = total;
        this.status = "PLACED";
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDelivery() { return delivery; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
}
