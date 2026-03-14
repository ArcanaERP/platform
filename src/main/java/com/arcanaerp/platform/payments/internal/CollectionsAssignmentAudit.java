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
    name = "collections_assignment_audits",
    indexes = {
        @Index(name = "idx_caa_tenant_invoice_assigned", columnList = "tenantCode,invoiceNumber,assignedAt"),
        @Index(name = "idx_caa_tenant_assigned_to", columnList = "tenantCode,assignedTo")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CollectionsAssignmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String invoiceNumber;

    @Column(nullable = false, length = 128)
    private String assignedTo;

    @Column(nullable = false, length = 128)
    private String assignedBy;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    private CollectionsAssignmentAudit(
        UUID id,
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        String assignedBy,
        Instant assignedAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.invoiceNumber = invoiceNumber;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    static CollectionsAssignmentAudit create(
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        String assignedBy,
        Instant assignedAt
    ) {
        if (assignedAt == null) {
            throw new IllegalArgumentException("assignedAt is required");
        }
        return new CollectionsAssignmentAudit(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase(),
            normalizeActorEmail(assignedTo, "assignedTo"),
            normalizeActorEmail(assignedBy, "assignedBy"),
            assignedAt
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
