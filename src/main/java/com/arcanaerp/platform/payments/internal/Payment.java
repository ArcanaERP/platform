package com.arcanaerp.platform.payments.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "payments",
    uniqueConstraints = @UniqueConstraint(name = "uk_payments_reference", columnNames = "paymentReference")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String paymentReference;

    @Column(nullable = false, length = 64)
    private String invoiceNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false)
    private Instant paidAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Payment(
        UUID id,
        String tenantCode,
        String paymentReference,
        String invoiceNumber,
        BigDecimal amount,
        String currencyCode,
        Instant paidAt,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.paymentReference = paymentReference;
        this.invoiceNumber = invoiceNumber;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
    }

    static Payment create(
        String tenantCode,
        String paymentReference,
        String invoiceNumber,
        BigDecimal amount,
        String currencyCode,
        Instant paidAt,
        Instant createdAt
    ) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (paidAt == null) {
            throw new IllegalArgumentException("paidAt is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new Payment(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(paymentReference, "paymentReference").toUpperCase(),
            normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase(),
            amount,
            normalizeCurrencyCode(currencyCode),
            paidAt,
            createdAt
        );
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
