package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryFacilityPartyRoleAssignmentRepository
    extends JpaRepository<InventoryFacilityPartyRoleAssignment, UUID> {

    @Query(
        """
        select count(assignment) > 0
        from InventoryFacilityPartyRoleAssignment assignment
        where assignment.inventoryFacilityId = :inventoryFacilityId
          and assignment.partyCode = :partyCode
          and assignment.roleTypeCode = :roleTypeCode
          and ((:fromDate is null and assignment.fromDate is null) or assignment.fromDate = :fromDate)
          and ((:thruDate is null and assignment.thruDate is null) or assignment.thruDate = :thruDate)
        """
    )
    boolean existsExactAssignment(
        @Param("inventoryFacilityId") UUID inventoryFacilityId,
        @Param("partyCode") String partyCode,
        @Param("roleTypeCode") String roleTypeCode,
        @Param("fromDate") Instant fromDate,
        @Param("thruDate") Instant thruDate
    );

    @Query(
        """
        select count(assignment) > 0
        from InventoryFacilityPartyRoleAssignment assignment
        where assignment.inventoryFacilityId = :inventoryFacilityId
          and assignment.partyCode = :partyCode
          and assignment.roleTypeCode = :roleTypeCode
          and assignment.active = true
          and (:thruDate is null or assignment.fromDate is null or assignment.fromDate <= :thruDate)
          and (:fromDate is null or assignment.thruDate is null or assignment.thruDate >= :fromDate)
        """
    )
    boolean existsOverlappingActiveAssignment(
        @Param("inventoryFacilityId") UUID inventoryFacilityId,
        @Param("partyCode") String partyCode,
        @Param("roleTypeCode") String roleTypeCode,
        @Param("fromDate") Instant fromDate,
        @Param("thruDate") Instant thruDate
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
