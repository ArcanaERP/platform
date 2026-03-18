package com.arcanaerp.platform.payments.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CollectionsFollowUpAuditRepository extends JpaRepository<CollectionsFollowUpAudit, UUID> {

    @Query(
        """
        select audit
        from CollectionsFollowUpAudit audit
        where audit.tenantCode = :tenantCode
          and audit.invoiceNumber = :invoiceNumber
        """
    )
    Page<CollectionsFollowUpAudit> findHistory(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        Pageable pageable
    );
}
