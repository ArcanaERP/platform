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
    name = "work_effort_good_standards",
    indexes = {
        @Index(name = "idx_wegs_work_effort", columnList = "workEffortId"),
        @Index(name = "idx_wegs_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_wegs_good_type", columnList = "goodTypeId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortGoodStandard {

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
    private Long goodTypeId;

    private BigDecimal estimatedQuantity;

    private Long estimatedCostMoneyId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortGoodStandard create(
        WorkEffort workEffort,
        Long goodTypeId,
        BigDecimal estimatedQuantity,
        Long estimatedCostMoneyId,
        Instant createdAt
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (goodTypeId == null) {
            throw new IllegalArgumentException("goodTypeId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortGoodStandard standard = new WorkEffortGoodStandard();
        standard.workEffortId = workEffort.getId();
        standard.tenantCode = workEffort.getTenantCode();
        standard.effortNumber = workEffort.getEffortNumber();
        standard.goodTypeId = goodTypeId;
        standard.estimatedQuantity = estimatedQuantity;
        standard.estimatedCostMoneyId = estimatedCostMoneyId;
        standard.createdAt = createdAt;
        return standard;
    }
}
