package com.arcanaerp.platform.devsupport.internal;

import com.arcanaerp.platform.devsupport.NoticeSeverity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SystemNoticeRepository extends JpaRepository<SystemNotice, UUID> {

    Optional<SystemNotice> findByTenantCodeAndNoticeCode(String tenantCode, String noticeCode);

    Page<SystemNotice> findByTenantCode(String tenantCode, Pageable pageable);

    Page<SystemNotice> findByTenantCodeAndSeverity(String tenantCode, NoticeSeverity severity, Pageable pageable);

    Page<SystemNotice> findByTenantCodeAndActive(String tenantCode, boolean active, Pageable pageable);

    Page<SystemNotice> findByTenantCodeAndSeverityAndActive(
        String tenantCode,
        NoticeSeverity severity,
        boolean active,
        Pageable pageable
    );
}
