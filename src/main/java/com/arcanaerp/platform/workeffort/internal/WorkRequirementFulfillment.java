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
    name = "work_requirement_fulfillment",
    indexes = {
        @Index(name = "idx_wrf_work_effort_requirement", columnList = "workEffortId,requirementId"),
        @Index(name = "idx_wrf_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_wrf_requirement", columnList = "requirementId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkRequirementFulfillment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID workEffortId;

    @Column(nullable = false, length = 64)
    private String tenantCode;

    @Column(nullable = false, length = 64)
    private String effortNumber;

    @Column(nullable = false)
    private Long requirementId;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkRequirementFulfillment create(
        WorkEffort workEffort,
        Long requirementId,
        String description,
        Instant createdAt
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (requirementId == null) {
            throw new IllegalArgumentException("requirementId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkRequirementFulfillment fulfillment = new WorkRequirementFulfillment();
        fulfillment.workEffortId = workEffort.getId();
        fulfillment.tenantCode = workEffort.getTenantCode();
        fulfillment.effortNumber = workEffort.getEffortNumber();
        fulfillment.requirementId = requirementId;
        fulfillment.description = normalizeOptional(description);
        fulfillment.createdAt = createdAt;
        return fulfillment;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
