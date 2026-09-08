package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryFacilityPartyRoleAssignmentRepository
    extends JpaRepository<InventoryFacilityPartyRoleAssignment, UUID> {

    Optional<InventoryFacilityPartyRoleAssignment> findByInventoryFacilityIdAndPartyCodeAndRoleTypeCode(
        UUID inventoryFacilityId,
        String partyCode,
        String roleTypeCode
    );

    @Query(
        """
        select assignment
        from InventoryFacilityPartyRoleAssignment assignment
        where (:facilityCode is null or assignment.facilityCode = :facilityCode)
          and (:partyCode is null or assignment.partyCode = :partyCode)
          and (:roleTypeCode is null or assignment.roleTypeCode = :roleTypeCode)
          and (:assignedBy is null or assignment.assignedBy = :assignedBy)
          and (:active is null or assignment.active = :active)
        """
    )
    Page<InventoryFacilityPartyRoleAssignment> findAssignmentsFiltered(
        @Param("facilityCode") String facilityCode,
        @Param("partyCode") String partyCode,
        @Param("roleTypeCode") String roleTypeCode,
        @Param("assignedBy") String assignedBy,
        @Param("active") Boolean active,
        Pageable pageable
    );
}
