package com.arcanaerp.platform.agreements.internal;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AgreementRepository extends JpaRepository<Agreement, UUID> {

    Optional<Agreement> findByAgreementNumber(String agreementNumber);

    Page<Agreement> findByTenantCode(String tenantCode, Pageable pageable);

    Page<Agreement> findByStatus(AgreementStatus status, Pageable pageable);

    Page<Agreement> findByTenantCodeAndStatus(String tenantCode, AgreementStatus status, Pageable pageable);
}
