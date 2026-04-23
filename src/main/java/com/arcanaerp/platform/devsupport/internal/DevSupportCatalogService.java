package com.arcanaerp.platform.devsupport.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.devsupport.DiagnosticRunLogView;
import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
import com.arcanaerp.platform.devsupport.DevSupportCatalog;
import com.arcanaerp.platform.devsupport.MaintenanceWindowView;
import com.arcanaerp.platform.devsupport.RegisterDiagnosticRunLogCommand;
import com.arcanaerp.platform.devsupport.NoticeSeverity;
import com.arcanaerp.platform.devsupport.RegisterMaintenanceWindowCommand;
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
    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final DiagnosticRunLogRepository diagnosticRunLogRepository;
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

    @Override
    public MaintenanceWindowView registerMaintenanceWindow(RegisterMaintenanceWindowCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String windowCode = normalizeRequired(command.windowCode(), "windowCode").toUpperCase();
        Instant now = Instant.now(clock);

        if (maintenanceWindowRepository.findByTenantCodeAndWindowCode(tenantCode, windowCode).isPresent()) {
            throw new ConflictException("Maintenance window already exists for tenant/code: " + tenantCode + "/" + windowCode);
        }

        MaintenanceWindow window = maintenanceWindowRepository.save(
            MaintenanceWindow.create(
                tenantCode,
                windowCode,
                command.title(),
                command.description(),
                command.startsAt(),
                command.endsAt(),
                command.active(),
                now
            )
        );
        return toView(window);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceWindowView getMaintenanceWindow(String tenantCode, String windowCode) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedWindowCode = normalizeRequired(windowCode, "windowCode").toUpperCase();
        MaintenanceWindow window = maintenanceWindowRepository.findByTenantCodeAndWindowCode(normalizedTenantCode, normalizedWindowCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Maintenance window not found for tenant/code: " + normalizedTenantCode + "/" + normalizedWindowCode
            ));
        return toView(window);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MaintenanceWindowView> listMaintenanceWindows(
        String tenantCode,
        PageQuery pageQuery,
        Boolean active,
        Instant startsAtFrom,
        Instant startsAtTo
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        Page<MaintenanceWindow> page = maintenanceWindowRepository.findFiltered(
            normalizedTenantCode,
            active,
            startsAtFrom,
            startsAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "startsAt"))
        );
        return PageResult.from(page).map(this::toView);
    }

    @Override
    public DiagnosticRunLogView registerDiagnosticRunLog(RegisterDiagnosticRunLogCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String runNumber = normalizeRequired(command.runNumber(), "runNumber").toUpperCase();
        Instant now = Instant.now(clock);

        if (diagnosticRunLogRepository.findByTenantCodeAndRunNumber(tenantCode, runNumber).isPresent()) {
            throw new ConflictException("Diagnostic run log already exists for tenant/runNumber: " + tenantCode + "/" + runNumber);
        }

        DiagnosticRunLog runLog = diagnosticRunLogRepository.save(
            DiagnosticRunLog.create(
                tenantCode,
                runNumber,
                command.diagnosticCode(),
                command.title(),
                command.summary(),
                command.status(),
                command.startedAt(),
                command.finishedAt(),
                now
            )
        );
        return toView(runLog);
    }

    @Override
    @Transactional(readOnly = true)
    public DiagnosticRunLogView getDiagnosticRunLog(String tenantCode, String runNumber) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedRunNumber = normalizeRequired(runNumber, "runNumber").toUpperCase();
        DiagnosticRunLog runLog = diagnosticRunLogRepository.findByTenantCodeAndRunNumber(normalizedTenantCode, normalizedRunNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Diagnostic run log not found for tenant/runNumber: " + normalizedTenantCode + "/" + normalizedRunNumber
            ));
        return toView(runLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DiagnosticRunLogView> listDiagnosticRunLogs(
        String tenantCode,
        PageQuery pageQuery,
        DiagnosticRunStatus status,
        Instant startedAtFrom,
        Instant startedAtTo
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        Page<DiagnosticRunLog> page = diagnosticRunLogRepository.findFiltered(
            normalizedTenantCode,
            status,
            startedAtFrom,
            startedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "startedAt"))
        );
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

    private MaintenanceWindowView toView(MaintenanceWindow window) {
        return new MaintenanceWindowView(
            window.getId(),
            window.getTenantCode(),
            window.getWindowCode(),
            window.getTitle(),
            window.getDescription(),
            window.getStartsAt(),
            window.getEndsAt(),
            window.isActive(),
            window.getCreatedAt()
        );
    }

    private DiagnosticRunLogView toView(DiagnosticRunLog runLog) {
        return new DiagnosticRunLogView(
            runLog.getId(),
            runLog.getTenantCode(),
            runLog.getRunNumber(),
            runLog.getDiagnosticCode(),
            runLog.getTitle(),
            runLog.getSummary(),
            runLog.getStatus(),
            runLog.getStartedAt(),
            runLog.getFinishedAt(),
            runLog.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
