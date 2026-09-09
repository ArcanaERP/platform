package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryFixedAssetPartyRoleAssignmentRepository
    extends JpaRepository<InventoryFixedAssetPartyRoleAssignment, UUID> {

    @Query(
        """
        select count(assignment) > 0
        from InventoryFixedAssetPartyRoleAssignment assignment
        where assignment.inventoryFixedAssetId = :inventoryFixedAssetId
          and assignment.partyCode = :partyCode
          and assignment.roleTypeCode = :roleTypeCode
          and ((:fromDate is null and assignment.fromDate is null) or assignment.fromDate = :fromDate)
          and ((:thruDate is null and assignment.thruDate is null) or assignment.thruDate = :thruDate)
        """
    )
    boolean existsExactAssignment(
        @Param("inventoryFixedAssetId") UUID inventoryFixedAssetId,
        @Param("partyCode") String partyCode,
        @Param("roleTypeCode") String roleTypeCode,
        @Param("fromDate") Instant fromDate,
        @Param("thruDate") Instant thruDate
    );

    @Query(
        """
        select count(assignment) > 0
        from InventoryFixedAssetPartyRoleAssignment assignment
        where assignment.inventoryFixedAssetId = :inventoryFixedAssetId
          and assignment.partyCode = :partyCode
          and assignment.roleTypeCode = :roleTypeCode
          and assignment.active = true
          and (:thruDate is null or assignment.fromDate is null or assignment.fromDate <= :thruDate)
          and (:fromDate is null or assignment.thruDate is null or assignment.thruDate >= :fromDate)
        """
    )
    boolean existsOverlappingActiveAssignment(
        @Param("inventoryFixedAssetId") UUID inventoryFixedAssetId,
        @Param("partyCode") String partyCode,
        @Param("roleTypeCode") String roleTypeCode,
        @Param("fromDate") Instant fromDate,
        @Param("thruDate") Instant thruDate
    );

    @Query(
        """
        select assignment
        from InventoryFixedAssetPartyRoleAssignment assignment
        where (:fixedAssetCode is null or assignment.fixedAssetCode = :fixedAssetCode)
          and (:partyCode is null or assignment.partyCode = :partyCode)
          and (:roleTypeCode is null or assignment.roleTypeCode = :roleTypeCode)
          and (:assignedBy is null or assignment.assignedBy = :assignedBy)
          and (:active is null or assignment.active = :active)
        """
    )
    Page<InventoryFixedAssetPartyRoleAssignment> findAssignmentsFiltered(
        @Param("fixedAssetCode") String fixedAssetCode,
        @Param("partyCode") String partyCode,
        @Param("roleTypeCode") String roleTypeCode,
        @Param("assignedBy") String assignedBy,
        @Param("active") Boolean active,
        Pageable pageable
    );
}
