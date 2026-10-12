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
    name = "work_effort_fixed_asset_serviced",
    indexes = {
        @Index(name = "idx_wefasvc_work_effort", columnList = "workEffortId"),
        @Index(name = "idx_wefasvc_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_wefasvc_fixed_asset", columnList = "fixedAssetId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortFixedAssetServiced {

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
    private Long fixedAssetId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortFixedAssetServiced create(
        WorkEffort workEffort,
        Long fixedAssetId,
        Instant createdAt
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (fixedAssetId == null) {
            throw new IllegalArgumentException("fixedAssetId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortFixedAssetServiced serviced = new WorkEffortFixedAssetServiced();
        serviced.workEffortId = workEffort.getId();
        serviced.tenantCode = workEffort.getTenantCode();
        serviced.effortNumber = workEffort.getEffortNumber();
        serviced.fixedAssetId = fixedAssetId;
        serviced.createdAt = createdAt;
        return serviced;
    }
}
