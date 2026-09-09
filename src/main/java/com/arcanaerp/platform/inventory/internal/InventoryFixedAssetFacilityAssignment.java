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
    name = "inventory_fixed_asset_facility_assignments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_ifafa_asset_facility_type",
        columnNames = {"inventoryFixedAssetId", "inventoryFacilityId", "assignmentType"}
    ),
    indexes = {
        @Index(name = "idx_ifafa_asset_code", columnList = "fixedAssetCode"),
        @Index(name = "idx_ifafa_facility_code", columnList = "facilityCode"),
        @Index(name = "idx_ifafa_assignment_type", columnList = "assignmentType"),
        @Index(name = "idx_ifafa_assigned_by", columnList = "assignedBy,assignedAt"),
        @Index(name = "idx_ifafa_active", columnList = "active"),
        @Index(name = "idx_ifafa_current_asset", columnList = "fixedAssetCode,active,fromDate,thruDate"),
        @Index(name = "idx_ifafa_overlap_asset", columnList = "inventoryFixedAssetId,assignmentType,active,fromDate,thruDate")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryFixedAssetFacilityAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID inventoryFixedAssetId;

    @Column(nullable = false)
    private UUID inventoryFacilityId;

    @Column(nullable = false, length = 64)
    private String fixedAssetCode;

    @Column(nullable = false, length = 64)
    private String facilityCode;

    @Column(nullable = false, length = 64)
    private String assignmentType;

    @Column(length = 1024)
    private String comments;

    private Instant fromDate;

    private Instant thruDate;

    @Column(nullable = false, length = 128)
    private String assignedBy;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 256)
    private String endReason;

    @Column(length = 128)
    private String endedBy;

    private Instant endedAt;

    static InventoryFixedAssetFacilityAssignment create(
        InventoryFixedAsset fixedAsset,
        InventoryFacility facility,
        String assignmentType,
        String comments,
        Instant fromDate,
        Instant thruDate,
        String assignedBy,
        Instant assignedAt
    ) {
        if (fixedAsset == null) {
            throw new IllegalArgumentException("fixed asset is required");
        }
        if (facility == null) {
            throw new IllegalArgumentException("facility is required");
        }
        if (assignedAt == null) {
            throw new IllegalArgumentException("assignedAt is required");
        }
        if (fromDate != null && thruDate != null && fromDate.isAfter(thruDate)) {
            throw new IllegalArgumentException("fromDate must be before or equal to thruDate");
        }
        InventoryFixedAssetFacilityAssignment assignment = new InventoryFixedAssetFacilityAssignment();
        assignment.inventoryFixedAssetId = fixedAsset.getId();
        assignment.inventoryFacilityId = facility.getId();
        assignment.fixedAssetCode = fixedAsset.getCode();
        assignment.facilityCode = facility.getCode();
        assignment.assignmentType = normalizeRequired(assignmentType, "assignmentType").toUpperCase();
        assignment.comments = normalizeOptional(comments);
        assignment.fromDate = fromDate;
        assignment.thruDate = thruDate;
        assignment.assignedBy = normalizeRequired(assignedBy, "assignedBy").toLowerCase();
        assignment.assignedAt = assignedAt;
        assignment.active = true;
        return assignment;
    }

    void end(Instant thruDate, String reason, String endedBy, Instant endedAt) {
        if (thruDate == null) {
            throw new IllegalArgumentException("thruDate is required");
        }
        if (endedAt == null) {
            throw new IllegalArgumentException("endedAt is required");
        }
        if (!active) {
            throw new IllegalArgumentException("Inventory fixed asset facility assignment is already ended");
        }
        if (fromDate != null && thruDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("thruDate must be after or equal to fromDate");
        }
        this.thruDate = thruDate;
        active = false;
        endReason = normalizeRequired(reason, "reason");
        this.endedBy = normalizeRequired(endedBy, "endedBy").toLowerCase();
        this.endedAt = endedAt;
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
