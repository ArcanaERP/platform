package com.arcanaerp.platform.orders.internal;

import com.arcanaerp.platform.orders.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "order_status_change_audits",
    indexes = {
        @Index(name = "idx_osca_order_changed", columnList = "salesOrderId,changedAt"),
        @Index(name = "idx_osca_order_current_changed", columnList = "salesOrderId,currentStatus,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrderStatusChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID salesOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus currentStatus;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 320)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    private OrderStatusChangeAudit(
        UUID id,
        UUID salesOrderId,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String reason,
        String changedBy,
        Instant changedAt
    ) {
        this.id = id;
        this.salesOrderId = salesOrderId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    static OrderStatusChangeAudit create(
        UUID salesOrderId,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String reason,
        String changedBy,
        Instant changedAt
    ) {
        if (salesOrderId == null) {
            throw new IllegalArgumentException("salesOrderId is required");
        }
        if (previousStatus == null) {
            throw new IllegalArgumentException("previousStatus is required");
        }
        if (currentStatus == null) {
            throw new IllegalArgumentException("currentStatus is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if (changedBy == null || changedBy.isBlank()) {
            throw new IllegalArgumentException("changedBy is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        return new OrderStatusChangeAudit(
            null,
            salesOrderId,
            previousStatus,
            currentStatus,
            reason.trim(),
            changedBy.trim().toLowerCase(),
            changedAt
        );
    }
}
