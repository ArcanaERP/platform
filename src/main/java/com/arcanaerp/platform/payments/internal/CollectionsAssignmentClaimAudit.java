package com.arcanaerp.platform.payments.internal;

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
    name = "collections_assignment_claim_audits",
    indexes = {
        @Index(name = "idx_caca_tenant_invoice_claimed", columnList = "tenantCode,invoiceNumber,claimedAt"),
        @Index(name = "idx_caca_tenant_claimed_by", columnList = "tenantCode,claimedBy")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CollectionsAssignmentClaimAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String invoiceNumber;

    @Column(nullable = false, length = 128)
    private String claimedBy;

    @Column(nullable = false, updatable = false)
    private Instant claimedAt;

    private CollectionsAssignmentClaimAudit(
        UUID id,
        String tenantCode,
        String invoiceNumber,
        String claimedBy,
        Instant claimedAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.invoiceNumber = invoiceNumber;
        this.claimedBy = claimedBy;
        this.claimedAt = claimedAt;
    }

    static CollectionsAssignmentClaimAudit create(
        String tenantCode,
        String invoiceNumber,
        String claimedBy,
        Instant claimedAt
    ) {
        if (claimedAt == null) {
            throw new IllegalArgumentException("claimedAt is required");
        }
        return new CollectionsAssignmentClaimAudit(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase(),
            normalizeActorEmail(claimedBy, "claimedBy"),
            claimedAt
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
