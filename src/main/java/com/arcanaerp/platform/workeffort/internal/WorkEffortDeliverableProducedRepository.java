package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortDeliverableProducedRepository extends JpaRepository<WorkEffortDeliverableProduced, UUID> {

    @Query(
        """
        select produced
        from WorkEffortDeliverableProduced produced
        where (:tenantCode is null or produced.tenantCode = :tenantCode)
          and (:effortNumber is null or produced.effortNumber = :effortNumber)
          and (:deliverableId is null or produced.deliverableId = :deliverableId)
        """
    )
    Page<WorkEffortDeliverableProduced> findProducedFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("deliverableId") Long deliverableId,
        Pageable pageable
    );
}
