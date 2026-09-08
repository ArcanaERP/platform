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
    name = "inventory_telecom_contacts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inventory_telecom_contacts_owner_purpose_type",
        columnNames = {"ownerType", "ownerCode", "contactPurposeCode", "telecomType"}
    ),
    indexes = {
        @Index(name = "idx_inventory_telecom_contacts_owner", columnList = "ownerType,ownerCode"),
        @Index(name = "idx_inventory_telecom_contacts_purpose", columnList = "contactPurposeCode"),
        @Index(name = "idx_inventory_telecom_contacts_type", columnList = "telecomType")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryTelecomContact {

    static final String TYPE_EMAIL = "EMAIL";
    static final String TYPE_PHONE = "PHONE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String ownerType;

    @Column(nullable = false, length = 64)
    private String ownerCode;

    @Column(nullable = false, length = 64)
    private String contactPurposeCode;

    @Column(nullable = false, length = 32)
    private String telecomType;

    @Column(length = 128)
    private String contactName;

    @Column(nullable = false, length = 255)
    private String contactValue;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    static InventoryTelecomContact create(
        String ownerType,
        String ownerCode,
        String contactPurposeCode,
        String telecomType,
        String contactName,
        String contactValue,
        Instant createdAt
    ) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        InventoryTelecomContact contact = new InventoryTelecomContact();
        contact.ownerType = normalizeRequired(ownerType, "ownerType").toUpperCase();
        contact.ownerCode = normalizeRequired(ownerCode, "ownerCode").toUpperCase();
        contact.contactPurposeCode = normalizeRequired(contactPurposeCode, "contactPurposeCode").toUpperCase();
        contact.telecomType = normalizeTelecomType(telecomType);
        contact.contactName = normalizeOptional(contactName);
        contact.contactValue = normalizeContactValue(contact.telecomType, contactValue);
        contact.createdAt = createdAt;
        contact.updatedAt = createdAt;
        return contact;
    }

    void updateMetadata(
        String contactPurposeCode,
        String telecomType,
        String contactName,
        String contactValue,
        Instant updatedAt
    ) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
        String normalizedContactPurposeCode = normalizeRequired(contactPurposeCode, "contactPurposeCode").toUpperCase();
        String normalizedTelecomType = normalizeTelecomType(telecomType);
        String normalizedContactName = normalizeOptional(contactName);
        String normalizedContactValue = normalizeContactValue(normalizedTelecomType, contactValue);

        if (
            this.contactPurposeCode.equals(normalizedContactPurposeCode)
                && this.telecomType.equals(normalizedTelecomType)
                && equalsNullable(this.contactName, normalizedContactName)
                && this.contactValue.equals(normalizedContactValue)
        ) {
            throw new IllegalArgumentException("Inventory telecom contact metadata is unchanged");
        }

        this.contactPurposeCode = normalizedContactPurposeCode;
        this.telecomType = normalizedTelecomType;
        this.contactName = normalizedContactName;
        this.contactValue = normalizedContactValue;
        this.updatedAt = updatedAt;
    }

    static String normalizeTelecomType(String value) {
        String normalized = normalizeRequired(value, "telecomType").toUpperCase();
        if (!TYPE_EMAIL.equals(normalized) && !TYPE_PHONE.equals(normalized)) {
            throw new IllegalArgumentException("telecomType must be EMAIL or PHONE");
        }
        return normalized;
    }

    private static String normalizeContactValue(String telecomType, String value) {
        String normalized = normalizeRequired(value, "contactValue");
        return TYPE_EMAIL.equals(telecomType) ? normalized.toLowerCase() : normalized;
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

    private static boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
