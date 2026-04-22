package com.arcanaerp.platform.workeffort.internal;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "work_efforts",
    uniqueConstraints = @UniqueConstraint(name = "uk_work_efforts_tenant_effort_number", columnNames = {"tenantCode", "effortNumber"}),
    indexes = {
        @Index(name = "idx_work_efforts_tenant_created_at", columnList = "tenantCode,createdAt"),
        @Index(name = "idx_work_efforts_tenant_status", columnList = "tenantCode,status"),
        @Index(name = "idx_work_efforts_tenant_assigned_to", columnList = "tenantCode,assignedTo")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffort {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String effortNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkEffortStatus status;

    @Column(nullable = false, length = 320)
    private String assignedTo;

    @Column
    private Instant dueAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private WorkEffort(
        UUID id,
        String tenantCode,
        String effortNumber,
        String name,
        String description,
        WorkEffortStatus status,
        String assignedTo,
        Instant dueAt,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.effortNumber = effortNumber;
        this.name = name;
        this.description = description;
        this.status = status;
        this.assignedTo = assignedTo;
        this.dueAt = dueAt;
        this.createdAt = createdAt;
    }

    static WorkEffort create(
        String tenantCode,
        String effortNumber,
        String name,
        String description,
        WorkEffortStatus status,
        String assignedTo,
        Instant dueAt,
        Instant createdAt
    ) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        return new WorkEffort(
            null,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(effortNumber, "effortNumber").toUpperCase(),
            normalizeRequired(name, "name"),
            normalizeRequired(description, "description"),
            status,
            normalizeEmail(assignedTo),
            dueAt,
            createdAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeRequired(value, "assignedTo").toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("assignedTo is invalid");
        }
        return normalized;
    }
}
