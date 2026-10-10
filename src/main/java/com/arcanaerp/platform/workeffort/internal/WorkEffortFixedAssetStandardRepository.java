package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortFixedAssetStandardRepository extends JpaRepository<WorkEffortFixedAssetStandard, UUID> {

    @Query(
        """
        select standard
        from WorkEffortFixedAssetStandard standard
        where (:tenantCode is null or standard.tenantCode = :tenantCode)
          and (:effortNumber is null or standard.effortNumber = :effortNumber)
          and (:fixedAssetTypeId is null or standard.fixedAssetTypeId = :fixedAssetTypeId)
          and (:estimatedCostMoneyId is null or standard.estimatedCostMoneyId = :estimatedCostMoneyId)
        """
    )
    Page<WorkEffortFixedAssetStandard> findStandardsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("fixedAssetTypeId") Long fixedAssetTypeId,
        @Param("estimatedCostMoneyId") Long estimatedCostMoneyId,
        Pageable pageable
    );
}
