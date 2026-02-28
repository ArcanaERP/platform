package com.arcanaerp.platform.orders.internal;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, UUID> {

    List<SalesOrderLine> findBySalesOrderIdOrderByLineNoAsc(UUID salesOrderId);

    List<SalesOrderLine> findBySalesOrderIdInOrderBySalesOrderIdAscLineNoAsc(Set<UUID> salesOrderIds);
}
