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
    name = "order_requirement_commitments",
    indexes = {
        @Index(name = "idx_orc_order_line_requirement", columnList = "orderLineItemId,requirementId"),
        @Index(name = "idx_orc_requirement", columnList = "requirementId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrderRequirementCommitment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long orderLineItemId;

    @Column(nullable = false)
    private Long requirementId;

    @Column(length = 512)
    private String description;

    private Integer quantity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static OrderRequirementCommitment create(
        Long orderLineItemId,
        Long requirementId,
        String description,
        Integer quantity,
        Instant createdAt
    ) {
        if (orderLineItemId == null) {
            throw new IllegalArgumentException("orderLineItemId is required");
        }
        if (requirementId == null) {
            throw new IllegalArgumentException("requirementId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        OrderRequirementCommitment commitment = new OrderRequirementCommitment();
        commitment.orderLineItemId = orderLineItemId;
        commitment.requirementId = requirementId;
        commitment.description = normalizeOptional(description);
        commitment.quantity = quantity;
        commitment.createdAt = createdAt;
        return commitment;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
