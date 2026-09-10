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
    name = "inventory_party_fixed_asset_assignments",
    indexes = {
        @Index(name = "idx_ipfaa_party_asset", columnList = "inventoryPartyId,inventoryFixedAssetId"),
        @Index(name = "idx_ipfaa_party_code", columnList = "partyCode"),
        @Index(name = "idx_ipfaa_asset_code", columnList = "fixedAssetCode"),
        @Index(name = "idx_ipfaa_assigned_from", columnList = "assignedFrom"),
        @Index(name = "idx_ipfaa_assigned_thru", columnList = "assignedThru"),
        @Index(name = "idx_ipfaa_cost_money", columnList = "allocatedCostMoneyId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryPartyFixedAssetAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryPartyId;

    @Column(nullable = false, length = 64)
    private String partyCode;

    @Column(nullable = false)
    private UUID inventoryFixedAssetId;

    @Column(nullable = false, length = 64)
    private String fixedAssetCode;

    private Instant assignedFrom;

    private Instant assignedThru;

    private Long allocatedCostMoneyId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    static InventoryPartyFixedAssetAssignment create(
        InventoryParty party,
        InventoryFixedAsset fixedAsset,
        Instant assignedFrom,
        Instant assignedThru,
        Long allocatedCostMoneyId,
        Instant createdAt
    ) {
        if (party == null) {
            throw new IllegalArgumentException("party is required");
        }
        if (fixedAsset == null) {
            throw new IllegalArgumentException("fixed asset is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        if (assignedFrom != null && assignedThru != null && assignedFrom.isAfter(assignedThru)) {
            throw new IllegalArgumentException("assignedFrom must be before or equal to assignedThru");
        }

        InventoryPartyFixedAssetAssignment assignment = new InventoryPartyFixedAssetAssignment();
        assignment.inventoryPartyId = party.getId();
        assignment.partyCode = party.getCode();
        assignment.inventoryFixedAssetId = fixedAsset.getId();
        assignment.fixedAssetCode = fixedAsset.getCode();
        assignment.assignedFrom = assignedFrom;
        assignment.assignedThru = assignedThru;
        assignment.allocatedCostMoneyId = allocatedCostMoneyId;
        assignment.createdAt = createdAt;
        return assignment;
    }
}
