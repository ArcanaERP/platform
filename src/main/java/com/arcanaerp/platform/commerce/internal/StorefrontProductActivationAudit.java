package com.arcanaerp.platform.commerce.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "commerce_storefront_product_activation_audits",
    indexes = {
        @Index(name = "idx_cspa_product_changed", columnList = "storefrontProductId,changedAt"),
        @Index(name = "idx_cspa_product_active_changed", columnList = "storefrontProductId,currentActive,changedAt"),
        @Index(name = "idx_cspa_product_tenant_changed", columnList = "storefrontProductId,tenantCode,changedAt"),
        @Index(name = "idx_cspa_product_actor_changed", columnList = "storefrontProductId,changedBy,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class StorefrontProductActivationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID storefrontProductId;

    @Column(nullable = false)
    private boolean previousActive;

    @Column(nullable = false)
    private boolean currentActive;

    @Column(nullable = false, length = 512)
    private String reason;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 320)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    private StorefrontProductActivationAudit(
        UUID id,
        UUID storefrontProductId,
        boolean previousActive,
        boolean currentActive,
        String reason,
        String tenantCode,
        String changedBy,
        Instant changedAt
    ) {
        this.id = id;
        this.storefrontProductId = storefrontProductId;
        this.previousActive = previousActive;
        this.currentActive = currentActive;
        this.reason = reason;
        this.tenantCode = tenantCode;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    static StorefrontProductActivationAudit create(
        UUID storefrontProductId,
        boolean previousActive,
        boolean currentActive,
        String reason,
        String tenantCode,
        String changedBy,
        Instant changedAt
    ) {
        if (storefrontProductId == null) {
            throw new IllegalArgumentException("storefrontProductId is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        return new StorefrontProductActivationAudit(
            null,
            storefrontProductId,
            previousActive,
            currentActive,
            normalizeRequired(reason, "reason"),
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(changedBy, "changedBy").toLowerCase(),
            changedAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
