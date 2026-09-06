package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryProductInstanceAssignmentRepository
    extends JpaRepository<InventoryProductInstanceAssignment, UUID> {

    Optional<InventoryProductInstanceAssignment> findByInventoryItemIdAndProductInstanceCode(
        UUID inventoryItemId,
        String productInstanceCode
    );

    @Query(
        """
        select assignment
        from InventoryProductInstanceAssignment assignment
        where (:sku is null or assignment.sku = :sku)
          and (:locationCode is null or assignment.locationCode = :locationCode)
          and (:productInstanceCode is null or assignment.productInstanceCode = :productInstanceCode)
          and (:assignedBy is null or assignment.assignedBy = :assignedBy)
        """
    )
    Page<InventoryProductInstanceAssignment> findAssignmentsFiltered(
        @Param("sku") String sku,
        @Param("locationCode") String locationCode,
        @Param("productInstanceCode") String productInstanceCode,
        @Param("assignedBy") String assignedBy,
        Pageable pageable
    );
}
