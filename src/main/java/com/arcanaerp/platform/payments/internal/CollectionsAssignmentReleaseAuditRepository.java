package com.arcanaerp.platform.payments.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CollectionsAssignmentReleaseAuditRepository extends JpaRepository<CollectionsAssignmentReleaseAudit, UUID> {

    @Query(
        """
        select audit
        from CollectionsAssignmentReleaseAudit audit
        where audit.tenantCode = :tenantCode
          and audit.invoiceNumber = :invoiceNumber
        """
    )
    Page<CollectionsAssignmentReleaseAudit> findHistory(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        Pageable pageable
    );
}
