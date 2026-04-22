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
    name = "commerce_storefront_products",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_commerce_storefront_products_tenant_storefront_sku",
        columnNames = {"tenantCode", "storefrontCode", "sku"}
    ),
    indexes = {
        @Index(name = "idx_commerce_storefront_products_storefront_position", columnList = "tenantCode,storefrontCode,position"),
        @Index(name = "idx_commerce_storefront_products_storefront_active", columnList = "tenantCode,storefrontCode,active")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class StorefrontProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID storefrontId;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String storefrontCode;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(length = 255)
    private String merchandisingName;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private StorefrontProduct(
        UUID id,
        UUID storefrontId,
        String tenantCode,
        String storefrontCode,
        String sku,
        String merchandisingName,
        int position,
        boolean active,
        Instant createdAt
    ) {
        this.id = id;
        this.storefrontId = storefrontId;
        this.tenantCode = tenantCode;
        this.storefrontCode = storefrontCode;
        this.sku = sku;
        this.merchandisingName = merchandisingName;
        this.position = position;
        this.active = active;
        this.createdAt = createdAt;
    }

    static StorefrontProduct create(
        UUID storefrontId,
        String tenantCode,
        String storefrontCode,
        String sku,
        String merchandisingName,
        int position,
        boolean active,
        Instant createdAt
    ) {
        if (storefrontId == null) {
            throw new IllegalArgumentException("storefrontId is required");
        }
        if (position < 0) {
            throw new IllegalArgumentException("position must be greater than or equal to zero");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new StorefrontProduct(
            null,
            storefrontId,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(storefrontCode, "storefrontCode").toUpperCase(),
            normalizeRequired(sku, "sku").toUpperCase(),
            normalizeOptional(merchandisingName),
            position,
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

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
