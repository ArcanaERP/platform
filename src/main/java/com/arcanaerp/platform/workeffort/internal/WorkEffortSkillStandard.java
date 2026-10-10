package com.arcanaerp.platform.workeffort.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "work_effort_skill_standards",
    indexes = {
        @Index(name = "idx_wess_work_effort", columnList = "workEffortId"),
        @Index(name = "idx_wess_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_wess_skill_type", columnList = "skillTypeId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortSkillStandard {

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
    private Long skillTypeId;

    private BigDecimal estimatedNumPeople;

    private BigDecimal estimatedDuration;

    private Long estimatedCostMoneyId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortSkillStandard create(
        WorkEffort workEffort,
        Long skillTypeId,
        BigDecimal estimatedNumPeople,
        BigDecimal estimatedDuration,
        Long estimatedCostMoneyId,
        Instant createdAt
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (skillTypeId == null) {
            throw new IllegalArgumentException("skillTypeId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortSkillStandard standard = new WorkEffortSkillStandard();
        standard.workEffortId = workEffort.getId();
        standard.tenantCode = workEffort.getTenantCode();
        standard.effortNumber = workEffort.getEffortNumber();
        standard.skillTypeId = skillTypeId;
        standard.estimatedNumPeople = estimatedNumPeople;
        standard.estimatedDuration = estimatedDuration;
        standard.estimatedCostMoneyId = estimatedCostMoneyId;
        standard.createdAt = createdAt;
        return standard;
    }
}
