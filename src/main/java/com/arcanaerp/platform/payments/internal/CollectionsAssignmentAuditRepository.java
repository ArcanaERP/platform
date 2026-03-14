package com.arcanaerp.platform.payments.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CollectionsAssignmentAuditRepository extends JpaRepository<CollectionsAssignmentAudit, UUID> {

    Page<CollectionsAssignmentAudit> findByTenantCodeAndInvoiceNumber(
        String tenantCode,
        String invoiceNumber,
        Pageable pageable
    );
}
