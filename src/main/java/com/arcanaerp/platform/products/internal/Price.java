package com.arcanaerp.platform.products.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "product_prices",
    indexes = @Index(name = "idx_product_prices_product_effective", columnList = "productId,effectiveFrom")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Price {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, updatable = false)
    private Instant effectiveFrom;

    private Price(UUID id, UUID productId, BigDecimal amount, String currencyCode, Instant effectiveFrom) {
        this.id = id;
        this.productId = productId;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.effectiveFrom = effectiveFrom;
    }

    static Price create(UUID productId, BigDecimal amount, String currencyCode, Instant effectiveFrom) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        return new Price(
            null,
            productId,
            amount,
            normalizeCurrencyCode(currencyCode),
            effectiveFrom
        );
    }

    private static String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("currencyCode is required");
        }
        String normalized = currencyCode.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code");
        }
        return normalized;
    }
}
