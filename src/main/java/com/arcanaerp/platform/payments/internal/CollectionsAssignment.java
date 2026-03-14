package com.arcanaerp.platform.payments.internal;

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
    name = "collections_assignments",
    uniqueConstraints = @UniqueConstraint(name = "uk_collections_assignment_invoice", columnNames = "invoiceNumber"),
    indexes = {
        @Index(name = "idx_collections_assignment_tenant_assigned_to", columnList = "tenantCode,assignedTo"),
        @Index(name = "idx_collections_assignment_tenant_assigned_at", columnList = "tenantCode,assignedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CollectionsAssignment {

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

    @Column(nullable = false)
    private Instant assignedAt;

    private CollectionsAssignment(
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

    static CollectionsAssignment create(
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        String assignedBy,
        Instant assignedAt
    ) {
        if (assignedAt == null) {
            throw new IllegalArgumentException("assignedAt is required");
        }
        return new CollectionsAssignment(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase(),
            normalizeActorEmail(assignedTo, "assignedTo"),
            normalizeActorEmail(assignedBy, "assignedBy"),
            assignedAt
        );
    }

    CollectionsAssignment reassign(
        String assignedTo,
        String assignedBy,
        Instant assignedAt
    ) {
        if (assignedAt == null) {
            throw new IllegalArgumentException("assignedAt is required");
        }
        this.assignedTo = normalizeActorEmail(assignedTo, "assignedTo");
        this.assignedBy = normalizeActorEmail(assignedBy, "assignedBy");
        this.assignedAt = assignedAt;
        return this;
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
