package com.arcanaerp.platform.workeffort.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "role_types_work_efforts",
    indexes = {
        @Index(name = "idx_rtwe_role_effort", columnList = "roleTypeCode,workEffortId"),
        @Index(name = "idx_rtwe_tenant_effort", columnList = "tenantCode,effortNumber")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortRoleTypeAssignment {

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
    private String roleTypeCode;

    static WorkEffortRoleTypeAssignment create(WorkEffort workEffort, String roleTypeCode) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        WorkEffortRoleTypeAssignment assignment = new WorkEffortRoleTypeAssignment();
        assignment.workEffortId = workEffort.getId();
        assignment.tenantCode = workEffort.getTenantCode();
        assignment.effortNumber = workEffort.getEffortNumber();
        assignment.roleTypeCode = normalizeRequired(roleTypeCode, "roleTypeCode").toUpperCase();
        return assignment;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
