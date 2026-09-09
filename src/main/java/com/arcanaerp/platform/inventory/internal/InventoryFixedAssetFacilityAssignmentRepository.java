package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryFixedAssetFacilityAssignmentRepository
    extends JpaRepository<InventoryFixedAssetFacilityAssignment, UUID> {

    Optional<InventoryFixedAssetFacilityAssignment> findByInventoryFixedAssetIdAndInventoryFacilityIdAndAssignmentType(
        UUID inventoryFixedAssetId,
        UUID inventoryFacilityId,
        String assignmentType
    );

    @Query(
        """
        select count(assignment) > 0
        from InventoryFixedAssetFacilityAssignment assignment
        where assignment.inventoryFixedAssetId = :inventoryFixedAssetId
          and assignment.assignmentType = :assignmentType
          and assignment.active = true
          and (:thruDate is null or assignment.fromDate is null or assignment.fromDate <= :thruDate)
          and (:fromDate is null or assignment.thruDate is null or assignment.thruDate >= :fromDate)
        """
    )
    boolean existsOverlappingActiveAssignment(
        @Param("inventoryFixedAssetId") UUID inventoryFixedAssetId,
        @Param("assignmentType") String assignmentType,
        @Param("fromDate") Instant fromDate,
        @Param("thruDate") Instant thruDate
    );

    @Query(
        """
        select assignment
        from InventoryFixedAssetFacilityAssignment assignment
        where (:fixedAssetCode is null or assignment.fixedAssetCode = :fixedAssetCode)
          and (:facilityCode is null or assignment.facilityCode = :facilityCode)
          and (:assignmentType is null or assignment.assignmentType = :assignmentType)
          and (:assignedBy is null or assignment.assignedBy = :assignedBy)
          and (:active is null or assignment.active = :active)
        """
    )
    Page<InventoryFixedAssetFacilityAssignment> findAssignmentsFiltered(
        @Param("fixedAssetCode") String fixedAssetCode,
        @Param("facilityCode") String facilityCode,
        @Param("assignmentType") String assignmentType,
        @Param("assignedBy") String assignedBy,
        @Param("active") Boolean active,
        Pageable pageable
    );

    @Query(
        """
        select assignment
        from InventoryFixedAssetFacilityAssignment assignment
        where assignment.active = true
          and (:fixedAssetCode is null or assignment.fixedAssetCode = :fixedAssetCode)
          and (:facilityCode is null or assignment.facilityCode = :facilityCode)
          and (:assignmentType is null or assignment.assignmentType = :assignmentType)
          and (assignment.fromDate is null or assignment.fromDate <= :effectiveAt)
          and (assignment.thruDate is null or assignment.thruDate >= :effectiveAt)
        """
    )
    Page<InventoryFixedAssetFacilityAssignment> findCurrentAssignmentsFiltered(
        @Param("fixedAssetCode") String fixedAssetCode,
        @Param("facilityCode") String facilityCode,
        @Param("assignmentType") String assignmentType,
        @Param("effectiveAt") Instant effectiveAt,
        Pageable pageable
    );
}
