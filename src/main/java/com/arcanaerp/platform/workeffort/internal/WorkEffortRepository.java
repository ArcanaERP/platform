package com.arcanaerp.platform.workeffort.internal;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface WorkEffortRepository extends JpaRepository<WorkEffort, UUID> {

    Optional<WorkEffort> findByTenantCodeAndEffortNumber(String tenantCode, String effortNumber);

    Page<WorkEffort> findByTenantCode(String tenantCode, Pageable pageable);

    Page<WorkEffort> findByTenantCodeAndStatus(String tenantCode, WorkEffortStatus status, Pageable pageable);

    Page<WorkEffort> findByTenantCodeAndAssignedTo(String tenantCode, String assignedTo, Pageable pageable);

    Page<WorkEffort> findByTenantCodeAndStatusAndAssignedTo(
        String tenantCode,
        WorkEffortStatus status,
        String assignedTo,
        Pageable pageable
    );
}
