package com.arcanaerp.platform.orders.internal;

import com.arcanaerp.platform.orders.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OrderStatusChangeAuditRepository extends JpaRepository<OrderStatusChangeAudit, UUID> {

    Page<OrderStatusChangeAudit> findBySalesOrderId(UUID salesOrderId, Pageable pageable);

    @Query(
        """
        select audit
        from OrderStatusChangeAudit audit
        where audit.salesOrderId = :salesOrderId
          and (:previousStatus is null or audit.previousStatus = :previousStatus)
          and (:currentStatus is null or audit.currentStatus = :currentStatus)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    Page<OrderStatusChangeAudit> findHistoryFiltered(
        @Param("salesOrderId") UUID salesOrderId,
        @Param("previousStatus") OrderStatus previousStatus,
        @Param("currentStatus") OrderStatus currentStatus,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo,
        Pageable pageable
    );
}
