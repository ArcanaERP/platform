package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortFixedAssetServicedRepository extends JpaRepository<WorkEffortFixedAssetServiced, UUID> {

    @Query(
        """
        select serviced
        from WorkEffortFixedAssetServiced serviced
        where (:tenantCode is null or serviced.tenantCode = :tenantCode)
          and (:effortNumber is null or serviced.effortNumber = :effortNumber)
          and (:fixedAssetId is null or serviced.fixedAssetId = :fixedAssetId)
        """
    )
    Page<WorkEffortFixedAssetServiced> findServicedFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("fixedAssetId") Long fixedAssetId,
        Pageable pageable
    );
}
