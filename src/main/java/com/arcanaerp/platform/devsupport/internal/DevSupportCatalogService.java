package com.arcanaerp.platform.devsupport.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.devsupport.DevSupportCatalog;
import com.arcanaerp.platform.devsupport.NoticeSeverity;
import com.arcanaerp.platform.devsupport.RegisterSystemNoticeCommand;
import com.arcanaerp.platform.devsupport.SystemNoticeView;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class DevSupportCatalogService implements DevSupportCatalog {

    private final SystemNoticeRepository systemNoticeRepository;
    private final Clock clock;

    @Override
    public SystemNoticeView registerSystemNotice(RegisterSystemNoticeCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String noticeCode = normalizeRequired(command.noticeCode(), "noticeCode").toUpperCase();
        Instant now = Instant.now(clock);

        if (systemNoticeRepository.findByTenantCodeAndNoticeCode(tenantCode, noticeCode).isPresent()) {
            throw new ConflictException("System notice already exists for tenant/code: " + tenantCode + "/" + noticeCode);
        }

        SystemNotice notice = systemNoticeRepository.save(
            SystemNotice.create(
                tenantCode,
                noticeCode,
                command.title(),
                command.message(),
                command.severity(),
                command.active(),
                now
            )
        );
        return toView(notice);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemNoticeView getSystemNotice(String tenantCode, String noticeCode) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedNoticeCode = normalizeRequired(noticeCode, "noticeCode").toUpperCase();
        SystemNotice notice = systemNoticeRepository.findByTenantCodeAndNoticeCode(normalizedTenantCode, normalizedNoticeCode)
            .orElseThrow(() -> new NoSuchElementException(
                "System notice not found for tenant/code: " + normalizedTenantCode + "/" + normalizedNoticeCode
            ));
        return toView(notice);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SystemNoticeView> listSystemNotices(
        String tenantCode,
        PageQuery pageQuery,
        NoticeSeverity severity,
        Boolean active
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        Page<SystemNotice> page = findNotices(normalizedTenantCode, severity, active, pageQuery);
        return PageResult.from(page).map(this::toView);
    }

    private Page<SystemNotice> findNotices(String tenantCode, NoticeSeverity severity, Boolean active, PageQuery pageQuery) {
        var pageable = pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (severity != null && active != null) {
            return systemNoticeRepository.findByTenantCodeAndSeverityAndActive(tenantCode, severity, active, pageable);
        }
        if (severity != null) {
            return systemNoticeRepository.findByTenantCodeAndSeverity(tenantCode, severity, pageable);
        }
        if (active != null) {
            return systemNoticeRepository.findByTenantCodeAndActive(tenantCode, active, pageable);
        }
        return systemNoticeRepository.findByTenantCode(tenantCode, pageable);
    }

    private SystemNoticeView toView(SystemNotice notice) {
        return new SystemNoticeView(
            notice.getId(),
            notice.getTenantCode(),
            notice.getNoticeCode(),
            notice.getTitle(),
            notice.getMessage(),
            notice.getSeverity(),
            notice.isActive(),
            notice.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
