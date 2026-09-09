package com.arcanaerp.platform.inventory.internal;

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
}
