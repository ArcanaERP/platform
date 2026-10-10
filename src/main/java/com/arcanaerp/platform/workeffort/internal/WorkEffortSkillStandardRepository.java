package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortSkillStandardRepository extends JpaRepository<WorkEffortSkillStandard, UUID> {

    @Query(
        """
        select standard
        from WorkEffortSkillStandard standard
        where (:tenantCode is null or standard.tenantCode = :tenantCode)
          and (:effortNumber is null or standard.effortNumber = :effortNumber)
          and (:skillTypeId is null or standard.skillTypeId = :skillTypeId)
          and (:estimatedCostMoneyId is null or standard.estimatedCostMoneyId = :estimatedCostMoneyId)
        """
    )
    Page<WorkEffortSkillStandard> findStandardsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("skillTypeId") Long skillTypeId,
        @Param("estimatedCostMoneyId") Long estimatedCostMoneyId,
        Pageable pageable
    );
}
