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
    name = "work_order_item_fulfillments",
    indexes = {
        @Index(name = "idx_woif_work_effort_order_line", columnList = "workEffortId,orderLineItemId"),
        @Index(name = "idx_woif_tenant_effort", columnList = "tenantCode,effortNumber"),
        @Index(name = "idx_woif_order_line", columnList = "orderLineItemId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WorkOrderItemFulfillment {

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
    private Long orderLineItemId;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static WorkOrderItemFulfillment create(
        WorkEffort workEffort,
        Long orderLineItemId,
        String description,
        Instant createdAt
    ) {
        if (workEffort == null) {
            throw new IllegalArgumentException("workEffort is required");
        }
        if (orderLineItemId == null) {
            throw new IllegalArgumentException("orderLineItemId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        WorkOrderItemFulfillment fulfillment = new WorkOrderItemFulfillment();
        fulfillment.workEffortId = workEffort.getId();
        fulfillment.tenantCode = workEffort.getTenantCode();
        fulfillment.effortNumber = workEffort.getEffortNumber();
        fulfillment.orderLineItemId = orderLineItemId;
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
