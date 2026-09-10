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
    name = "work_effort_fixed_asset_assignments",
    indexes = {
        @Index(name = "idx_wefaa_work_effort_asset", columnList = "workEffortId,fixedAssetCode"),
        @Index(name = "idx_wefaa_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_wefaa_fixed_asset", columnList = "fixedAssetCode")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortFixedAssetAssignment {

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
    private String fixedAssetCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortFixedAssetAssignment create(WorkEffort workEffort, String fixedAssetCode, Instant createdAt) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortFixedAssetAssignment assignment = new WorkEffortFixedAssetAssignment();
        assignment.workEffortId = workEffort.getId();
        assignment.tenantCode = workEffort.getTenantCode();
        assignment.effortNumber = workEffort.getEffortNumber();
        assignment.fixedAssetCode = normalizeRequired(fixedAssetCode, "fixedAssetCode").toUpperCase();
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
