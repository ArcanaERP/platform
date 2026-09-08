package com.arcanaerp.platform.inventory.internal;

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
    name = "inventory_telecom_contact_metadata_change_audits",
    indexes = {
        @Index(name = "idx_itcmca_contact_changed", columnList = "telecomContactId,changedAt"),
        @Index(name = "idx_itcmca_owner_changed", columnList = "ownerType,ownerCode,changedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryTelecomContactMetadataChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID telecomContactId;

    @Column(nullable = false, length = 32)
    private String ownerType;

    @Column(nullable = false, length = 64)
    private String ownerCode;

    @Column(nullable = false, length = 64)
    private String previousContactPurposeCode;

    @Column(nullable = false, length = 64)
    private String currentContactPurposeCode;

    @Column(nullable = false, length = 32)
    private String previousTelecomType;

    @Column(nullable = false, length = 32)
    private String currentTelecomType;

    @Column(length = 128)
    private String previousContactName;

    @Column(length = 128)
    private String currentContactName;

    @Column(nullable = false, length = 255)
    private String previousContactValue;

    @Column(nullable = false, length = 255)
    private String currentContactValue;

    @Column(nullable = false, length = 128)
    private String changedBy;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    static InventoryTelecomContactMetadataChangeAudit create(
        UUID telecomContactId,
        String ownerType,
        String ownerCode,
        InventoryTelecomContactMetadataSnapshot previous,
        InventoryTelecomContactMetadataSnapshot current,
        String changedBy,
        Instant changedAt
    ) {
        if (telecomContactId == null) {
            throw new IllegalArgumentException("telecomContactId is required");
        }
        if (previous == null) {
            throw new IllegalArgumentException("previous is required");
        }
        if (current == null) {
            throw new IllegalArgumentException("current is required");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        InventoryTelecomContactMetadataChangeAudit audit = new InventoryTelecomContactMetadataChangeAudit();
        audit.telecomContactId = telecomContactId;
        audit.ownerType = normalizeRequired(ownerType, "ownerType").toUpperCase();
        audit.ownerCode = normalizeRequired(ownerCode, "ownerCode").toUpperCase();
        audit.previousContactPurposeCode = normalizeRequired(
            previous.contactPurposeCode(),
            "previousContactPurposeCode"
        ).toUpperCase();
        audit.currentContactPurposeCode = normalizeRequired(
            current.contactPurposeCode(),
            "currentContactPurposeCode"
        ).toUpperCase();
        audit.previousTelecomType = InventoryTelecomContact.normalizeTelecomType(previous.telecomType());
        audit.currentTelecomType = InventoryTelecomContact.normalizeTelecomType(current.telecomType());
        audit.previousContactName = normalizeOptional(previous.contactName());
        audit.currentContactName = normalizeOptional(current.contactName());
        audit.previousContactValue = normalizeRequired(previous.contactValue(), "previousContactValue");
        audit.currentContactValue = normalizeRequired(current.contactValue(), "currentContactValue");
        audit.changedBy = normalizeRequired(changedBy, "changedBy").toLowerCase();
        audit.changedAt = changedAt;
        return audit;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
