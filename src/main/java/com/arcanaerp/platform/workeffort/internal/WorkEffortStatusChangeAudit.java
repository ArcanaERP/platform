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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "work_effort_status_change_audits",
    indexes = {
        @Index(name = "idx_work_effort_status_audit_effort_changed", columnList = "workEffortId,changedAt"),
        @Index(name = "idx_work_effort_status_audit_tenant_changed", columnList = "tenantCode,changedAt"),
        @Index(name = "idx_work_effort_status_audit_tenant_actor", columnList = "tenantCode,changedBy")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortStatusChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID workEffortId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkEffortStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkEffortStatus currentStatus;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 320)
    private String changedBy;

    @Column(nullable = false)
    private Instant changedAt;

    private WorkEffortStatusChangeAudit(
        UUID id,
        UUID workEffortId,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String tenantCode,
        String reason,
        String changedBy,
        Instant changedAt
    ) {
        this.id = id;
        this.workEffortId = workEffortId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.tenantCode = tenantCode;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    static WorkEffortStatusChangeAudit create(
        UUID workEffortId,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String tenantCode,
        String reason,
        String changedBy,
        Instant changedAt
    ) {
        if (workEffortId == null) {
            throw new IllegalArgumentException("workEffortId is required");
        }
        if (previousStatus == null) {
            throw new IllegalArgumentException("previousStatus is required");
        }
        if (currentStatus == null) {
            throw new IllegalArgumentException("currentStatus is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        return new WorkEffortStatusChangeAudit(
            null,
            workEffortId,
            previousStatus,
            currentStatus,
            normalizeRequired(tenantCode, "tenantCode").toUpperCase(),
            normalizeRequired(reason, "reason"),
            normalizeEmail(changedBy),
            changedAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeRequired(value, "changedBy").toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("changedBy is invalid");
        }
        return normalized;
    }
}
