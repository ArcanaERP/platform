package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AssociatedWorkEffortRepository extends JpaRepository<AssociatedWorkEffort, UUID> {

    @Query(
        """
        select associatedWorkEffort
        from AssociatedWorkEffort associatedWorkEffort
        where (:tenantCode is null or associatedWorkEffort.tenantCode = :tenantCode)
          and (:effortNumber is null or associatedWorkEffort.effortNumber = :effortNumber)
          and (:associatedRecordId is null or associatedWorkEffort.associatedRecordId = :associatedRecordId)
          and (:associatedRecordType is null or associatedWorkEffort.associatedRecordType = :associatedRecordType)
        """
    )
    Page<AssociatedWorkEffort> findAssociatedWorkEffortsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("effortNumber") String effortNumber,
        @Param("associatedRecordId") Long associatedRecordId,
        @Param("associatedRecordType") String associatedRecordType,
        Pageable pageable
    );
}
