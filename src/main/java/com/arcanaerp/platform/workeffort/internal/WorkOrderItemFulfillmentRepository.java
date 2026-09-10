package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkOrderItemFulfillmentRepository extends JpaRepository<WorkOrderItemFulfillment, UUID> {

    @Query(
        """
        select fulfillment
        from WorkOrderItemFulfillment fulfillment
        where (:tenantCode is null or fulfillment.tenantCode = :tenantCode)
          and (:effortNumber is null or fulfillment.effortNumber = :effortNumber)
          and (:orderLineItemId is null or fulfillment.orderLineItemId = :orderLineItemId)
        """
    )
    Page<WorkOrderItemFulfillment> findFulfillmentsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("orderLineItemId") Long orderLineItemId,
        Pageable pageable
    );
}
