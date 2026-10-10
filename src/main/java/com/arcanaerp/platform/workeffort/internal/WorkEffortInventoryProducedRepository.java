package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortInventoryProducedRepository extends JpaRepository<WorkEffortInventoryProduced, UUID> {

    @Query(
        """
        select produced
        from WorkEffortInventoryProduced produced
        where (:tenantCode is null or produced.tenantCode = :tenantCode)
          and (:effortNumber is null or produced.effortNumber = :effortNumber)
          and (:inventoryEntryId is null or produced.inventoryEntryId = :inventoryEntryId)
        """
    )
    Page<WorkEffortInventoryProduced> findProducedFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("inventoryEntryId") Long inventoryEntryId,
        Pageable pageable
    );
}
