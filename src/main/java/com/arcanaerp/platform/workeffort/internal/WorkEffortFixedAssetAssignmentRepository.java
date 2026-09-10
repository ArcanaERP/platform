package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortFixedAssetAssignmentRepository extends JpaRepository<WorkEffortFixedAssetAssignment, UUID> {

    @Query(
        """
        select assignment
        from WorkEffortFixedAssetAssignment assignment
        where (:tenantCode is null or assignment.tenantCode = :tenantCode)
          and (:effortNumber is null or assignment.effortNumber = :effortNumber)
          and (:fixedAssetCode is null or assignment.fixedAssetCode = :fixedAssetCode)
        """
    )
    Page<WorkEffortFixedAssetAssignment> findAssignmentsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("fixedAssetCode") String fixedAssetCode,
        Pageable pageable
    );
}
