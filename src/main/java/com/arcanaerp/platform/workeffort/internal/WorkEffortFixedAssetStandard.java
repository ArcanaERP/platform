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
    name = "work_effort_fixed_asset_standards",
    indexes = {
        @Index(name = "idx_wefas_work_effort", columnList = "workEffortId"),
        @Index(name = "idx_wefas_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_wefas_fixed_asset_type", columnList = "fixedAssetTypeId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortFixedAssetStandard {

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
    private Long fixedAssetTypeId;

    private BigDecimal estimatedQuantity;

    private BigDecimal estimatedDuration;

    private Long estimatedCostMoneyId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortFixedAssetStandard create(
        WorkEffort workEffort,
        Long fixedAssetTypeId,
        BigDecimal estimatedQuantity,
        BigDecimal estimatedDuration,
        Long estimatedCostMoneyId,
        Instant createdAt
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (fixedAssetTypeId == null) {
            throw new IllegalArgumentException("fixedAssetTypeId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortFixedAssetStandard standard = new WorkEffortFixedAssetStandard();
        standard.workEffortId = workEffort.getId();
        standard.tenantCode = workEffort.getTenantCode();
        standard.effortNumber = workEffort.getEffortNumber();
        standard.fixedAssetTypeId = fixedAssetTypeId;
        standard.estimatedQuantity = estimatedQuantity;
        standard.estimatedDuration = estimatedDuration;
        standard.estimatedCostMoneyId = estimatedCostMoneyId;
        standard.createdAt = createdAt;
        return standard;
    }
}
