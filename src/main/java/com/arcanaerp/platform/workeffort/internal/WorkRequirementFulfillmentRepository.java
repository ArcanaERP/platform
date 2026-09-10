package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkRequirementFulfillmentRepository extends JpaRepository<WorkRequirementFulfillment, UUID> {

    @Query(
        """
        select fulfillment
        from WorkRequirementFulfillment fulfillment
        where (:tenantCode is null or fulfillment.tenantCode = :tenantCode)
          and (:effortNumber is null or fulfillment.effortNumber = :effortNumber)
          and (:requirementId is null or fulfillment.requirementId = :requirementId)
        """
    )
    Page<WorkRequirementFulfillment> findFulfillmentsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("requirementId") Long requirementId,
        Pageable pageable
    );
}
