package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WorkOrderItemRepository extends JpaRepository<WorkOrderItem, UUID> {
}
