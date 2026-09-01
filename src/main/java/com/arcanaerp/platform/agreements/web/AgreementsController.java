package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementManagement;
import com.arcanaerp.platform.agreements.AgreementStatus;
import com.arcanaerp.platform.agreements.AgreementStatusChangeView;
import com.arcanaerp.platform.agreements.AgreementView;
import com.arcanaerp.platform.agreements.ChangeAgreementStatusCommand;
import com.arcanaerp.platform.agreements.CreateAgreementCommand;
import com.arcanaerp.platform.agreements.DailyAgreementStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.agreements.DailyAgreementStatusActivitySummaryView;
import com.arcanaerp.platform.agreements.MonthlyAgreementStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.agreements.MonthlyAgreementStatusActivitySummaryView;
import com.arcanaerp.platform.agreements.WeeklyAgreementStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.agreements.WeeklyAgreementStatusActivitySummaryView;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agreements")
@RequiredArgsConstructor
public class AgreementsController {

    private final AgreementManagement agreementManagement;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgreementResponse createAgreement(@Valid @RequestBody CreateAgreementRequest request) {
        AgreementView agreement = agreementManagement.createAgreement(
            new CreateAgreementCommand(
                request.tenantCode(),
                request.agreementNumber(),
                request.name(),
                request.agreementType(),
                request.effectiveFrom()
            )
        );
        return toResponse(agreement);
    }

    @GetMapping("/{agreementNumber}")
    public AgreementResponse getAgreement(@PathVariable String agreementNumber) {
        return toResponse(agreementManagement.getAgreement(agreementNumber));
    }

    @GetMapping
    public PageResult<AgreementResponse> listAgreements(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String status
    ) {
        return agreementManagement.listAgreements(
            normalizeOptionalTenantCode(tenantCode),
            PageQuery.of(page, size),
            parseOptionalStatus(status)
        ).map(this::toResponse);
    }

    @GetMapping("/status-activity/daily-summary")
    public PageResult<DailyAgreementStatusActivitySummaryResponse> listDailyStatusActivitySummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listDailyStatusActivitySummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toDailyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/daily-summary/by-current-status")
    public PageResult<DailyAgreementStatusActivityByCurrentStatusSummaryResponse> listDailyStatusActivityByCurrentStatusSummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listDailyStatusActivityByCurrentStatusSummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toDailyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/status-activity/weekly-summary")
    public PageResult<WeeklyAgreementStatusActivitySummaryResponse> listWeeklyStatusActivitySummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listWeeklyStatusActivitySummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toWeeklyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/weekly-summary/by-current-status")
    public PageResult<WeeklyAgreementStatusActivityByCurrentStatusSummaryResponse> listWeeklyStatusActivityByCurrentStatusSummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listWeeklyStatusActivityByCurrentStatusSummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toWeeklyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/status-activity/monthly-summary")
    public PageResult<MonthlyAgreementStatusActivitySummaryResponse> listMonthlyStatusActivitySummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listMonthlyStatusActivitySummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMonthlyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/monthly-summary/by-current-status")
    public PageResult<MonthlyAgreementStatusActivityByCurrentStatusSummaryResponse> listMonthlyStatusActivityByCurrentStatusSummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listMonthlyStatusActivityByCurrentStatusSummaries(
            normalizeOptionalTenantCode(tenantCode),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMonthlyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/{agreementNumber}/status-history")
    public PageResult<AgreementStatusChangeResponse> listStatusHistory(
        @PathVariable String agreementNumber,
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return agreementManagement.listStatusHistory(
                agreementNumber,
                normalizeOptionalTenantCode(tenantCode),
                normalizeOptionalChangedBy(changedBy),
                parsedChangedAtFrom,
                parsedChangedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toStatusHistoryResponse);
    }

    @PatchMapping("/{agreementNumber}/status")
    public AgreementResponse changeAgreementStatus(
        @PathVariable String agreementNumber,
        @Valid @RequestBody ChangeAgreementStatusRequest request
    ) {
        AgreementView agreement = agreementManagement.changeAgreementStatus(
            new ChangeAgreementStatusCommand(
                agreementNumber,
                request.status(),
                request.tenantCode(),
                request.reason(),
                request.changedBy()
            )
        );
        return toResponse(agreement);
    }

    private AgreementResponse toResponse(AgreementView agreement) {
        return new AgreementResponse(
                agreement.id(),
                agreement.tenantCode(),
                agreement.agreementNumber(),
                agreement.name(),
                agreement.agreementType(),
                agreement.status(),
                agreement.effectiveFrom(),
                agreement.createdAt(),
                agreement.activatedAt(),
                agreement.terminatedAt()
            );
    }

    private AgreementStatusChangeResponse toStatusHistoryResponse(AgreementStatusChangeView change) {
        return new AgreementStatusChangeResponse(
            change.id(),
            change.agreementNumber(),
            change.previousStatus(),
            change.currentStatus(),
            change.tenantCode(),
            change.reason(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private DailyAgreementStatusActivitySummaryResponse toDailyStatusActivitySummaryResponse(
        DailyAgreementStatusActivitySummaryView view
    ) {
        return new DailyAgreementStatusActivitySummaryResponse(
            view.businessDate(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private DailyAgreementStatusActivityByCurrentStatusSummaryResponse toDailyStatusActivityByCurrentStatusSummaryResponse(
        DailyAgreementStatusActivityByCurrentStatusSummaryView view
    ) {
        return new DailyAgreementStatusActivityByCurrentStatusSummaryResponse(
            view.businessDate(),
            view.currentStatus(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private WeeklyAgreementStatusActivitySummaryResponse toWeeklyStatusActivitySummaryResponse(
        WeeklyAgreementStatusActivitySummaryView view
    ) {
        return new WeeklyAgreementStatusActivitySummaryResponse(
            view.businessWeekStart(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private WeeklyAgreementStatusActivityByCurrentStatusSummaryResponse toWeeklyStatusActivityByCurrentStatusSummaryResponse(
        WeeklyAgreementStatusActivityByCurrentStatusSummaryView view
    ) {
        return new WeeklyAgreementStatusActivityByCurrentStatusSummaryResponse(
            view.businessWeekStart(),
            view.currentStatus(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private MonthlyAgreementStatusActivitySummaryResponse toMonthlyStatusActivitySummaryResponse(
        MonthlyAgreementStatusActivitySummaryView view
    ) {
        return new MonthlyAgreementStatusActivitySummaryResponse(
            view.businessMonth(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private MonthlyAgreementStatusActivityByCurrentStatusSummaryResponse toMonthlyStatusActivityByCurrentStatusSummaryResponse(
        MonthlyAgreementStatusActivityByCurrentStatusSummaryView view
    ) {
        return new MonthlyAgreementStatusActivityByCurrentStatusSummaryResponse(
            view.businessMonth(),
            view.currentStatus(),
            view.transitionCount(),
            view.agreementCount()
        );
    }

    private static AgreementStatus parseOptionalStatus(String status) {
        return parseOptionalStatus(status, "status");
    }

    private static AgreementStatus parseOptionalStatus(String status, String parameterName) {
        if (status == null) {
            return null;
        }
        if (status.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        String normalized = status.trim().toUpperCase();
        try {
            return AgreementStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be one of: DRAFT, ACTIVE, TERMINATED");
        }
    }

    private static String normalizeOptionalTenantCode(String tenantCode) {
        if (tenantCode == null) {
            return null;
        }
        if (tenantCode.isBlank()) {
            throw new IllegalArgumentException("tenantCode query parameter must not be blank");
        }
        return tenantCode.trim().toUpperCase();
    }

    private static String normalizeOptionalChangedBy(String changedBy) {
        if (changedBy == null) {
            return null;
        }
        if (changedBy.isBlank()) {
            throw new IllegalArgumentException("changedBy query parameter must not be blank");
        }
        return changedBy.trim().toLowerCase();
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be a valid ISO-8601 instant");
        }
    }

    private static void validateChangedAtRange(Instant changedAtFrom, Instant changedAtTo) {
        if (changedAtFrom != null && changedAtTo != null && changedAtFrom.isAfter(changedAtTo)) {
            throw new IllegalArgumentException("changedAtFrom must be before or equal to changedAtTo");
        }
    }
}
