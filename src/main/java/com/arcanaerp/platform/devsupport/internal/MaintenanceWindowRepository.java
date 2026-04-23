package com.arcanaerp.platform.devsupport.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MaintenanceWindowRepository extends JpaRepository<MaintenanceWindow, UUID> {

    Optional<MaintenanceWindow> findByTenantCodeAndWindowCode(String tenantCode, String windowCode);

    @Query(
        """
        select maintenanceWindow
        from MaintenanceWindow maintenanceWindow
        where maintenanceWindow.tenantCode = :tenantCode
          and (:active is null or maintenanceWindow.active = :active)
          and (:startsAtFrom is null or maintenanceWindow.startsAt >= :startsAtFrom)
          and (:startsAtTo is null or maintenanceWindow.startsAt <= :startsAtTo)
        """
    )
    Page<MaintenanceWindow> findFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("active") Boolean active,
        @Param("startsAtFrom") Instant startsAtFrom,
        @Param("startsAtTo") Instant startsAtTo,
        Pageable pageable
    );
}
