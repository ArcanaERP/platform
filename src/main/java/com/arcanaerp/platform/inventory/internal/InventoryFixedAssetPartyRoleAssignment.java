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
    name = "inventory_fixed_asset_party_role_assignments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_ifapra_asset_party_role",
        columnNames = {"inventoryFixedAssetId", "partyCode", "roleTypeCode"}
    ),
    indexes = {
        @Index(name = "idx_ifapra_asset_code", columnList = "fixedAssetCode"),
        @Index(name = "idx_ifapra_party_role", columnList = "partyCode,roleTypeCode"),
        @Index(name = "idx_ifapra_assigned_by", columnList = "assignedBy,assignedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryFixedAssetPartyRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryFixedAssetId;

    @Column(nullable = false, length = 64)
    private String fixedAssetCode;

    @Column(nullable = false, length = 64)
    private String partyCode;

    @Column(nullable = false, length = 64)
    private String roleTypeCode;

    @Column(length = 1024)
    private String comments;

    private Instant fromDate;

    private Instant thruDate;

    @Column(nullable = false, length = 128)
    private String assignedBy;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    static InventoryFixedAssetPartyRoleAssignment create(
        InventoryFixedAsset fixedAsset,
        String partyCode,
        String roleTypeCode,
        String comments,
        Instant fromDate,
        Instant thruDate,
        String assignedBy,
        Instant assignedAt
    ) {
        if (fixedAsset == null) {
            throw new IllegalArgumentException("fixed asset is required");
        }
        if (assignedAt == null) {
            throw new IllegalArgumentException("assignedAt is required");
        }
        if (fromDate != null && thruDate != null && fromDate.isAfter(thruDate)) {
            throw new IllegalArgumentException("fromDate must be before or equal to thruDate");
        }
        InventoryFixedAssetPartyRoleAssignment assignment = new InventoryFixedAssetPartyRoleAssignment();
        assignment.inventoryFixedAssetId = fixedAsset.getId();
        assignment.fixedAssetCode = fixedAsset.getCode();
        assignment.partyCode = normalizeRequired(partyCode, "partyCode").toUpperCase();
        assignment.roleTypeCode = normalizeRequired(roleTypeCode, "roleTypeCode").toUpperCase();
        assignment.comments = normalizeOptional(comments);
        assignment.fromDate = fromDate;
        assignment.thruDate = thruDate;
        assignment.assignedBy = normalizeRequired(assignedBy, "assignedBy").toLowerCase();
        assignment.assignedAt = assignedAt;
        return assignment;
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
