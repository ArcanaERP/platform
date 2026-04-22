package com.arcanaerp.platform.commerce.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "commerce_storefronts",
    uniqueConstraints = @UniqueConstraint(name = "uk_commerce_storefronts_tenant_code", columnNames = {"tenantCode", "storefrontCode"}),
    indexes = {
        @Index(name = "idx_commerce_storefronts_tenant_created_at", columnList = "tenantCode,createdAt"),
        @Index(name = "idx_commerce_storefronts_tenant_active", columnList = "tenantCode,active")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Storefront {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String storefrontCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 16)
    private String defaultLanguageTag;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Storefront(
        UUID id,
        String tenantCode,
        String storefrontCode,
        String name,
        String currencyCode,
        String defaultLanguageTag,
        boolean active,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.storefrontCode = storefrontCode;
        this.name = name;
        this.currencyCode = currencyCode;
        this.defaultLanguageTag = defaultLanguageTag;
        this.active = active;
        this.createdAt = createdAt;
    }

    static Storefront create(
        String tenantCode,
        String storefrontCode,
        String name,
        String currencyCode,
        String defaultLanguageTag,
        boolean active,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new Storefront(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(storefrontCode, "storefrontCode").toUpperCase(),
            normalizeRequired(name, "name"),
            normalizeCurrencyCode(currencyCode),
            normalizeRequired(defaultLanguageTag, "defaultLanguageTag"),
            active,
            createdAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeCurrencyCode(String currencyCode) {
        String normalized = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code");
        }
        return normalized;
    }
}
