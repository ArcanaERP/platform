package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortGoodStandardRepository extends JpaRepository<WorkEffortGoodStandard, UUID> {

    @Query(
        """
        select standard
        from WorkEffortGoodStandard standard
        where (:tenantCode is null or standard.tenantCode = :tenantCode)
          and (:effortNumber is null or standard.effortNumber = :effortNumber)
          and (:goodTypeId is null or standard.goodTypeId = :goodTypeId)
          and (:estimatedCostMoneyId is null or standard.estimatedCostMoneyId = :estimatedCostMoneyId)
        """
    )
    Page<WorkEffortGoodStandard> findStandardsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("goodTypeId") Long goodTypeId,
        @Param("estimatedCostMoneyId") Long estimatedCostMoneyId,
        Pageable pageable
    );
}
