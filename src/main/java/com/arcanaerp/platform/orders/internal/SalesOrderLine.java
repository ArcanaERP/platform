package com.arcanaerp.platform.orders.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
    name = "sales_order_lines",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sales_order_lines_order_line_no",
        columnNames = {"salesOrderId", "lineNo"}
    ),
    indexes = @Index(name = "idx_sales_order_lines_order", columnList = "salesOrderId")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID salesOrderId;

    @Column(nullable = false)
    private int lineNo;

    @Column(nullable = false, length = 64)
    private String productSku;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private SalesOrderLine(
        UUID id,
        UUID salesOrderId,
        int lineNo,
        String productSku,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        Instant createdAt
    ) {
        this.id = id;
        this.salesOrderId = salesOrderId;
        this.lineNo = lineNo;
        this.productSku = productSku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
        this.createdAt = createdAt;
    }

    static SalesOrderLine create(
        UUID salesOrderId,
        int lineNo,
        String productSku,
        BigDecimal quantity,
        BigDecimal unitPrice,
        Instant createdAt
    ) {
        if (salesOrderId == null) {
            throw new IllegalArgumentException("salesOrderId is required");
        }
        if (lineNo < 1) {
            throw new IllegalArgumentException("lineNo must be greater than zero");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("unitPrice must be greater than zero");
        }
        BigDecimal lineTotal = quantity.multiply(unitPrice);

        return new SalesOrderLine(
            null,
            salesOrderId,
            lineNo,
            normalizeRequired(productSku, "productSku").toUpperCase(),
            quantity,
            unitPrice,
            lineTotal,
            createdAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
