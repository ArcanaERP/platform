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
    name = "work_effort_deliverable_produced",
    indexes = {
        @Index(name = "idx_wedp_work_effort", columnList = "workEffortId"),
        @Index(name = "idx_wedp_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_wedp_deliverable", columnList = "deliverableId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortDeliverableProduced {

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
    private Long deliverableId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortDeliverableProduced create(
        WorkEffort workEffort,
        Long deliverableId,
        Instant createdAt
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (deliverableId == null) {
            throw new IllegalArgumentException("deliverableId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortDeliverableProduced produced = new WorkEffortDeliverableProduced();
        produced.workEffortId = workEffort.getId();
        produced.tenantCode = workEffort.getTenantCode();
        produced.effortNumber = workEffort.getEffortNumber();
        produced.deliverableId = deliverableId;
        produced.createdAt = createdAt;
        return produced;
    }
}
