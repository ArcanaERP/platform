package com.arcanaerp.platform.invoicing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "invoice_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID invoiceId;

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

    private InvoiceLine(
        UUID id,
        UUID invoiceId,
        int lineNo,
        String productSku,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        Instant createdAt
    ) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.lineNo = lineNo;
        this.productSku = productSku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
        this.createdAt = createdAt;
    }

    static InvoiceLine create(
        UUID invoiceId,
        int lineNo,
        String productSku,
        BigDecimal quantity,
        BigDecimal unitPrice,
        Instant createdAt
    ) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("invoiceId is required");
        }
        if (lineNo <= 0) {
            throw new IllegalArgumentException("lineNo must be greater than zero");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("unitPrice must be greater than zero");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }

        return new InvoiceLine(
            null,
            invoiceId,
            lineNo,
            normalizeRequired(productSku, "productSku").toUpperCase(),
            quantity,
            unitPrice,
            quantity.multiply(unitPrice),
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
