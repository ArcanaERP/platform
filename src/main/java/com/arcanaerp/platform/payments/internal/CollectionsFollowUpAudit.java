package com.arcanaerp.platform.payments.internal;

import com.arcanaerp.platform.payments.CollectionsFollowUpOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "collections_follow_up_audits",
    indexes = {
        @Index(name = "idx_cfua_tenant_invoice_changed", columnList = "tenantCode,invoiceNumber,changedAt"),
        @Index(name = "idx_cfua_tenant_changed_by", columnList = "tenantCode,changedBy")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CollectionsFollowUpAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String invoiceNumber;

    private Instant previousFollowUpAt;

    private Instant followUpAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CollectionsFollowUpOutcome outcome;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    private CollectionsFollowUpAudit(
        UUID id,
        String tenantCode,
        String invoiceNumber,
        Instant previousFollowUpAt,
        Instant followUpAt,
        CollectionsFollowUpOutcome outcome,
        String changedBy,
        Instant changedAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.invoiceNumber = invoiceNumber;
        this.previousFollowUpAt = previousFollowUpAt;
        this.followUpAt = followUpAt;
        this.outcome = outcome;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    static CollectionsFollowUpAudit create(
        String tenantCode,
        String invoiceNumber,
        Instant previousFollowUpAt,
        Instant followUpAt,
        CollectionsFollowUpOutcome outcome,
        String changedBy,
        Instant changedAt
    ) {
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        return new CollectionsFollowUpAudit(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase(),
            previousFollowUpAt,
            followUpAt,
            outcome,
            normalizeActorEmail(changedBy, "changedBy"),
            changedAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeActorEmail(String value, String fieldName) {
        String normalized = normalizeRequired(value, fieldName).toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }
}
