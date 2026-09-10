package com.arcanaerp.platform.workeffort.internal;

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
    name = "work_effort_inventory_assignments",
    indexes = {
        @Index(name = "idx_weia_work_effort_inventory", columnList = "workEffortId,inventoryEntryCode"),
        @Index(name = "idx_weia_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_weia_inventory_entry", columnList = "inventoryEntryCode")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortInventoryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID workEffortId;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String effortNumber;

    @Column(nullable = false, length = 64)
    private String inventoryEntryCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortInventoryAssignment create(WorkEffort workEffort, String inventoryEntryCode, Instant createdAt) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortInventoryAssignment assignment = new WorkEffortInventoryAssignment();
        assignment.workEffortId = workEffort.getId();
        assignment.tenantCode = workEffort.getTenantCode();
        assignment.effortNumber = workEffort.getEffortNumber();
        assignment.inventoryEntryCode = normalizeRequired(inventoryEntryCode, "inventoryEntryCode").toUpperCase();
        assignment.createdAt = createdAt;
        return assignment;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
