package com.arcanaerp.platform.products.internal;

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
    name = "product_activation_audits",
    indexes = @Index(name = "idx_product_activation_audits_product_changed", columnList = "productId,changedAt")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductActivationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private boolean previousActive;

    @Column(nullable = false)
    private boolean currentActive;

    @Column(nullable = false, length = 512)
    private String reason;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    private ProductActivationAudit(
        UUID id,
        UUID productId,
        boolean previousActive,
        boolean currentActive,
        String reason,
        String tenantCode,
        String changedBy,
        Instant changedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.previousActive = previousActive;
        this.currentActive = currentActive;
        this.reason = reason;
        this.tenantCode = tenantCode;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    static ProductActivationAudit create(
        UUID productId,
        boolean previousActive,
        boolean currentActive,
        String reason,
        String tenantCode,
        String changedBy,
        Instant changedAt
    ) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalArgumentException("tenantCode is required");
        }
        if (changedBy == null || changedBy.isBlank()) {
            throw new IllegalArgumentException("changedBy is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        return new ProductActivationAudit(
            null,
            productId,
            previousActive,
            currentActive,
            reason.trim(),
            tenantCode.trim().toUpperCase(),
            changedBy.trim(),
            changedAt
        );
    }
}
