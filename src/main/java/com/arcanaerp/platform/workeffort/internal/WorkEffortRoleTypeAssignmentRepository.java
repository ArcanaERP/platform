package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortRoleTypeAssignmentRepository extends JpaRepository<WorkEffortRoleTypeAssignment, UUID> {

    @Query(
        """
        select assignment
        from WorkEffortRoleTypeAssignment assignment
        where (:tenantCode is null or assignment.tenantCode = :tenantCode)
          and (:effortNumber is null or assignment.effortNumber = :effortNumber)
          and (:roleTypeCode is null or assignment.roleTypeCode = :roleTypeCode)
        """
    )
    Page<WorkEffortRoleTypeAssignment> findAssignmentsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("roleTypeCode") String roleTypeCode,
        Pageable pageable
    );
}
