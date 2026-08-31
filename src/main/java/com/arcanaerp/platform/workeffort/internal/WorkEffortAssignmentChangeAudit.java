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
    name = "work_effort_assignment_change_audits",
    indexes = {
        @Index(name = "idx_work_effort_assignment_audit_effort_assigned", columnList = "workEffortId,assignedAt"),
        @Index(name = "idx_work_effort_assignment_audit_tenant_assigned", columnList = "tenantCode,assignedAt"),
        @Index(name = "idx_work_effort_assignment_audit_tenant_actor", columnList = "tenantCode,assignedBy")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortAssignmentChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID workEffortId;

    @Column(nullable = false, length = 320)
    private String previousAssignedTo;

    @Column(nullable = false, length = 320)
    private String currentAssignedTo;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 320)
    private String assignedBy;

    @Column(nullable = false)
    private Instant assignedAt;

    private WorkEffortAssignmentChangeAudit(
        UUID id,
        UUID workEffortId,
        String previousAssignedTo,
        String currentAssignedTo,
        String tenantCode,
        String reason,
        String assignedBy,
        Instant assignedAt
    ) {
        this.id = id;
        this.workEffortId = workEffortId;
        this.previousAssignedTo = previousAssignedTo;
        this.currentAssignedTo = currentAssignedTo;
        this.tenantCode = tenantCode;
        this.reason = reason;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    static WorkEffortAssignmentChangeAudit create(
        UUID workEffortId,
        String previousAssignedTo,
        String currentAssignedTo,
        String tenantCode,
        String reason,
        String assignedBy,
        Instant assignedAt
    ) {
        if (workEffortId == null) {
            throw new IllegalArgumentException("workEffortId is required");
        }
        if (assignedAt == null) {
            throw new IllegalArgumentException("assignedAt is required");
        }
        return new WorkEffortAssignmentChangeAudit(
            null,
            workEffortId,
            normalizeEmail(previousAssignedTo, "previousAssignedTo"),
            normalizeEmail(currentAssignedTo, "currentAssignedTo"),
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(reason, "reason"),
            normalizeEmail(assignedBy, "assignedBy"),
            assignedAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeEmail(String value, String fieldName) {
        String normalized = normalizeRequired(value, fieldName).toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }
}
