package com.arcanaerp.platform.orders.internal;

import com.arcanaerp.platform.orders.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "sales_orders",
    uniqueConstraints = @UniqueConstraint(name = "uk_sales_orders_order_number", columnNames = "orderNumber")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String orderNumber;

    @Column(nullable = false, length = 320)
    private String customerEmail;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private SalesOrder(
        UUID id,
        String orderNumber,
        String customerEmail,
        String currencyCode,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt
    ) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerEmail = customerEmail;
        this.currencyCode = currencyCode;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    static SalesOrder create(
        String orderNumber,
        String customerEmail,
        String currencyCode,
        BigDecimal totalAmount,
        Instant createdAt
    ) {
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("totalAmount must be greater than zero");
        }

        return new SalesOrder(
            null,
            normalizeRequired(orderNumber, "orderNumber").toUpperCase(),
            normalizeEmail(customerEmail),
            normalizeCurrencyCode(currencyCode),
            OrderStatus.DRAFT,
            totalAmount,
            createdAt
        );
    }

    void transitionTo(OrderStatus targetStatus) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (targetStatus == this.status) {
            return;
        }
        if (this.status != OrderStatus.DRAFT) {
            throw new IllegalArgumentException("Order status transition not allowed: " + this.status + " -> " + targetStatus);
        }
        if (targetStatus != OrderStatus.CONFIRMED && targetStatus != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order status transition not allowed: " + this.status + " -> " + targetStatus);
        }
        this.status = targetStatus;
    }

    private static String normalizeEmail(String email) {
        String normalized = normalizeRequired(email, "customerEmail").toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("customerEmail is invalid");
        }
        return normalized;
    }

    private static String normalizeCurrencyCode(String currencyCode) {
        String normalized = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code");
        }
        return normalized;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
