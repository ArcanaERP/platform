package com.arcanaerp.platform.workeffort.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "work_effort_party_assignments",
    indexes = {
        @Index(name = "idx_wepa_assigned_from", columnList = "assignedFrom"),
        @Index(name = "idx_wepa_assigned_thru", columnList = "assignedThru"),
        @Index(name = "idx_wepa_work_effort", columnList = "workEffortId"),
        @Index(name = "idx_wepa_party", columnList = "partyCode"),
        @Index(name = "idx_wepa_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_wepa_role_type", columnList = "roleTypeCode")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortPartyAssignment {

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
    private String partyCode;

    @Column(nullable = false, length = 64)
    private String roleTypeCode;

    private Instant assignedFrom;

    private Instant assignedThru;

    @Lob
    private String comments;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortPartyAssignment create(
        WorkEffort workEffort,
        String partyCode,
        String roleTypeCode,
        Instant assignedFrom,
        Instant assignedThru,
        String comments,
        Instant createdAt
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        if (assignedFrom != null && assignedThru != null && assignedFrom.isAfter(assignedThru)) {
            throw new IllegalArgumentException("assignedFrom must be before or equal to assignedThru");
        }
        WorkEffortPartyAssignment assignment = new WorkEffortPartyAssignment();
        assignment.workEffortId = workEffort.getId();
        assignment.tenantCode = workEffort.getTenantCode();
        assignment.effortNumber = workEffort.getEffortNumber();
        assignment.partyCode = normalizeRequired(partyCode, "partyCode").toUpperCase();
        assignment.roleTypeCode = normalizeRequired(roleTypeCode, "roleTypeCode").toUpperCase();
        assignment.assignedFrom = assignedFrom;
        assignment.assignedThru = assignedThru;
        assignment.comments = normalizeOptional(comments);
        assignment.createdAt = createdAt;
        return assignment;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
