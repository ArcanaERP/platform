package com.arcanaerp.platform.inventory.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryItemLocationAssignmentRepository extends JpaRepository<InventoryItemLocationAssignment, UUID> {

    @Query(
        """
        select assignment
        from InventoryItemLocationAssignment assignment
        where (:sku is null or assignment.sku = :sku)
          and (:itemLocationCode is null or assignment.itemLocationCode = :itemLocationCode)
          and (:assignedLocationCode is null or assignment.assignedLocationCode = :assignedLocationCode)
          and (:assignedFacilityCode is null or assignment.assignedFacilityCode = :assignedFacilityCode)
          and (:assignedStorageAreaCode is null or assignment.assignedStorageAreaCode = :assignedStorageAreaCode)
          and (:active is null or assignment.active = :active)
        """
    )
    Page<InventoryItemLocationAssignment> findAssignmentsFiltered(
        @Param("sku") String sku,
        @Param("itemLocationCode") String itemLocationCode,
        @Param("assignedLocationCode") String assignedLocationCode,
        @Param("assignedFacilityCode") String assignedFacilityCode,
        @Param("assignedStorageAreaCode") String assignedStorageAreaCode,
        @Param("active") Boolean active,
        Pageable pageable
    );
}
