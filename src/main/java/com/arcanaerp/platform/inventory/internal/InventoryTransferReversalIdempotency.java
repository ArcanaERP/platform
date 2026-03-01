package com.arcanaerp.platform.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inventory_transfer_reversal_idempotency",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_inventory_reversal_idempotency_transfer_key",
            columnNames = {"transferId", "idempotencyKey"}
        )
    },
    indexes = {
        @Index(name = "idx_inventory_reversal_idempotency_reversal_transfer", columnList = "reversalTransferId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryTransferReversalIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID transferId;

    @Column(nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Column(nullable = false)
    private UUID reversalTransferId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private InventoryTransferReversalIdempotency(
        UUID id,
        UUID transferId,
        String idempotencyKey,
        String requestFingerprint,
        UUID reversalTransferId,
        Instant createdAt
    ) {
        this.id = id;
        this.transferId = transferId;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.reversalTransferId = reversalTransferId;
        this.createdAt = createdAt;
    }

    static InventoryTransferReversalIdempotency create(
        UUID transferId,
        String idempotencyKey,
        String requestFingerprint,
        UUID reversalTransferId,
        Instant createdAt
    ) {
        if (transferId == null) {
            throw new IllegalArgumentException("transferId is required");
        }
        if (reversalTransferId == null) {
            throw new IllegalArgumentException("reversalTransferId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }

        return new InventoryTransferReversalIdempotency(
            null,
            transferId,
            normalizeRequired(idempotencyKey, "idempotencyKey"),
            normalizeRequired(requestFingerprint, "requestFingerprint"),
            reversalTransferId,
            createdAt
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
