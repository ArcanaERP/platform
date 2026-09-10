package com.arcanaerp.platform.workeffort.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortPartyAssignmentRepository extends JpaRepository<WorkEffortPartyAssignment, UUID> {

    @Query(
        """
        select assignment
        from WorkEffortPartyAssignment assignment
        where (:tenantCode is null or assignment.tenantCode = :tenantCode)
          and (:effortNumber is null or assignment.effortNumber = :effortNumber)
          and (:partyCode is null or assignment.partyCode = :partyCode)
          and (:roleTypeCode is null or assignment.roleTypeCode = :roleTypeCode)
          and (:assignedFrom is null or assignment.assignedFrom >= :assignedFrom)
          and (:assignedThru is null or assignment.assignedThru <= :assignedThru)
        """
    )
    Page<WorkEffortPartyAssignment> findAssignmentsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("partyCode") String partyCode,
        @Param("roleTypeCode") String roleTypeCode,
        @Param("assignedFrom") Instant assignedFrom,
        @Param("assignedThru") Instant assignedThru,
        Pageable pageable
    );
}
