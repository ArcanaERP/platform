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
    name = "inventory_facility_party_role_assignment_end_audits",
    indexes = {
        @Index(name = "idx_ifpraea_assignment_ended", columnList = "assignmentId,endedAt"),
        @Index(name = "idx_ifpraea_facility_role", columnList = "facilityCode,partyCode,roleTypeCode,endedAt"),
        @Index(name = "idx_ifpraea_ended_by", columnList = "endedBy,endedAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryFacilityPartyRoleAssignmentEndAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID assignmentId;

    @Column(nullable = false)
    private UUID inventoryFacilityId;

    @Column(nullable = false, length = 64)
    private String facilityCode;

    @Column(nullable = false, length = 64)
    private String partyCode;

    @Column(nullable = false, length = 64)
    private String roleTypeCode;

    private Instant previousThruDate;

    @Column(nullable = false)
    private Instant currentThruDate;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(nullable = false, length = 128)
    private String endedBy;

    @Column(nullable = false, updatable = false)
    private Instant endedAt;

    static InventoryFacilityPartyRoleAssignmentEndAudit create(
        InventoryFacilityPartyRoleAssignment assignment,
        Instant previousThruDate,
        String reason,
        String endedBy,
        Instant endedAt
    ) {
        if (assignment == null) {
            throw new IllegalArgumentException("assignment is required");
        }
        if (assignment.getThruDate() == null) {
            throw new IllegalArgumentException("currentThruDate is required");
        }
        if (endedAt == null) {
            throw new IllegalArgumentException("endedAt is required");
        }
        InventoryFacilityPartyRoleAssignmentEndAudit audit = new InventoryFacilityPartyRoleAssignmentEndAudit();
        audit.assignmentId = assignment.getId();
        audit.inventoryFacilityId = assignment.getInventoryFacilityId();
        audit.facilityCode = assignment.getFacilityCode();
        audit.partyCode = assignment.getPartyCode();
        audit.roleTypeCode = assignment.getRoleTypeCode();
        audit.previousThruDate = previousThruDate;
        audit.currentThruDate = assignment.getThruDate();
        audit.reason = normalizeRequired(reason, "reason");
        audit.endedBy = normalizeRequired(endedBy, "endedBy").toLowerCase();
        audit.endedAt = endedAt;
        return audit;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
