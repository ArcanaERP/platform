package com.arcanaerp.platform.agreements.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AgreementStatusChangeAuditRepository extends JpaRepository<AgreementStatusChangeAudit, UUID> {

    Page<AgreementStatusChangeAudit> findByAgreementId(UUID agreementId, Pageable pageable);
}
