package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortInventoryAssignmentRepository extends JpaRepository<WorkEffortInventoryAssignment, UUID> {

    @Query(
        """
        select assignment
        from WorkEffortInventoryAssignment assignment
        where (:tenantCode is null or assignment.tenantCode = :tenantCode)
          and (:effortNumber is null or assignment.effortNumber = :effortNumber)
          and (:inventoryEntryCode is null or assignment.inventoryEntryCode = :inventoryEntryCode)
        """
    )
    Page<WorkEffortInventoryAssignment> findAssignmentsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("inventoryEntryCode") String inventoryEntryCode,
        Pageable pageable
    );
}
