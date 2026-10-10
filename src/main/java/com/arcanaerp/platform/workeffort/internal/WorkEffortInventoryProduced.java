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
    name = "work_effort_inventory_produced",
    indexes = {
        @Index(name = "idx_weip_work_effort", columnList = "workEffortId"),
        @Index(name = "idx_weip_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_weip_inventory_entry", columnList = "inventoryEntryId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkEffortInventoryProduced {

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
    private Long inventoryEntryId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkEffortInventoryProduced create(
        WorkEffort workEffort,
        Long inventoryEntryId,
        Instant createdAt
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (inventoryEntryId == null) {
            throw new IllegalArgumentException("inventoryEntryId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkEffortInventoryProduced produced = new WorkEffortInventoryProduced();
        produced.workEffortId = workEffort.getId();
        produced.tenantCode = workEffort.getTenantCode();
        produced.effortNumber = workEffort.getEffortNumber();
        produced.inventoryEntryId = inventoryEntryId;
        produced.createdAt = createdAt;
        return produced;
    }
}
