package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.workeffort.AssignWorkEffortCommand;
import com.arcanaerp.platform.workeffort.ChangeWorkEffortStatusCommand;
import com.arcanaerp.platform.workeffort.CreateWorkEffortCommand;
import com.arcanaerp.platform.workeffort.DailyWorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.DailyWorkEffortStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.workeffort.DailyWorkEffortStatusActivitySummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortStatusActivitySummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortStatusActivitySummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentChangeView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentSummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortCatalog;
import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import com.arcanaerp.platform.workeffort.WorkEffortStatusChangeView;
import com.arcanaerp.platform.workeffort.WorkEffortView;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-efforts")
@RequiredArgsConstructor
public class WorkEffortsController {

    private final WorkEffortCatalog workEffortCatalog;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkEffortResponse createWorkEffort(@Valid @RequestBody CreateWorkEffortRequest request) {
        WorkEffortView created = workEffortCatalog.createWorkEffort(
            new CreateWorkEffortCommand(
                request.tenantCode(),
                request.effortNumber(),
                request.name(),
                request.description(),
                request.status(),
                request.assignedTo(),
                request.dueAt()
            )
        );
        return toResponse(created);
    }

    @GetMapping("/{effortNumber}")
    public WorkEffortResponse getWorkEffort(
        @PathVariable String effortNumber,
        @RequestParam String tenantCode
    ) {
        return toResponse(workEffortCatalog.getWorkEffort(tenantCode, effortNumber));
    }

    @GetMapping("/{effortNumber}/assignment")
    public WorkEffortAssignmentSummaryResponse getWorkEffortAssignment(
        @PathVariable String effortNumber,
        @RequestParam String tenantCode
    ) {
        return toAssignmentSummaryResponse(workEffortCatalog.getWorkEffortAssignment(tenantCode, effortNumber));
    }

    @PatchMapping("/{effortNumber}/status")
    public WorkEffortResponse changeWorkEffortStatus(
        @PathVariable String effortNumber,
        @Valid @RequestBody ChangeWorkEffortStatusRequest request
    ) {
        return toResponse(workEffortCatalog.changeWorkEffortStatus(
            new ChangeWorkEffortStatusCommand(
                request.tenantCode(),
                effortNumber,
                request.status(),
                request.reason(),
                request.changedBy()
            )
        ));
    }

    @PatchMapping("/{effortNumber}/assignment")
    public WorkEffortResponse assignWorkEffort(
        @PathVariable String effortNumber,
        @Valid @RequestBody AssignWorkEffortRequest request
    ) {
        return toResponse(workEffortCatalog.assignWorkEffort(
            new AssignWorkEffortCommand(
                request.tenantCode(),
                effortNumber,
                request.assignedTo(),
                request.reason(),
                request.assignedBy()
            )
        ));
    }

    @GetMapping
    public PageResult<WorkEffortResponse> listWorkEfforts(
        @RequestParam String tenantCode,
        @RequestParam(required = false) WorkEffortStatus status,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return workEffortCatalog.listWorkEfforts(
            tenantCode,
            PageQuery.of(page, size),
            status,
            normalizeOptionalAssignedTo(assignedTo)
        ).map(this::toResponse);
    }

    @GetMapping("/assignment-activity-summary")
    public PageResult<WorkEffortAssignmentActivitySummaryResponse> listAssignmentActivitySummaries(
        @RequestParam String tenantCode,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return workEffortCatalog.listAssignmentActivitySummaries(
            tenantCode,
            normalizeOptionalAssignedTo(assignedTo),
            parsedAssignedAtFrom,
            parsedAssignedAtTo,
            PageQuery.of(page, size)
        ).map(this::toAssignmentActivitySummaryResponse);
    }

    @GetMapping("/assignment-activity/daily-summary")
    public PageResult<DailyWorkEffortAssignmentActivitySummaryResponse> listDailyAssignmentActivitySummaries(
        @RequestParam String tenantCode,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return workEffortCatalog.listDailyAssignmentActivitySummaries(
            tenantCode,
            normalizeOptionalAssignedTo(assignedTo),
            parsedAssignedAtFrom,
            parsedAssignedAtTo,
            PageQuery.of(page, size)
        ).map(this::toDailyAssignmentActivitySummaryResponse);
    }

    @GetMapping("/assignment-activity/weekly-summary")
    public PageResult<WeeklyWorkEffortAssignmentActivitySummaryResponse> listWeeklyAssignmentActivitySummaries(
        @RequestParam String tenantCode,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return workEffortCatalog.listWeeklyAssignmentActivitySummaries(
            tenantCode,
            normalizeOptionalAssignedTo(assignedTo),
            parsedAssignedAtFrom,
            parsedAssignedAtTo,
            PageQuery.of(page, size)
        ).map(this::toWeeklyAssignmentActivitySummaryResponse);
    }

    @GetMapping("/assignment-activity/monthly-summary")
    public PageResult<MonthlyWorkEffortAssignmentActivitySummaryResponse> listMonthlyAssignmentActivitySummaries(
        @RequestParam String tenantCode,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return workEffortCatalog.listMonthlyAssignmentActivitySummaries(
            tenantCode,
            normalizeOptionalAssignedTo(assignedTo),
            parsedAssignedAtFrom,
            parsedAssignedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMonthlyAssignmentActivitySummaryResponse);
    }

    @GetMapping("/status-activity/daily-summary")
    public PageResult<DailyWorkEffortStatusActivitySummaryResponse> listDailyStatusActivitySummaries(
        @RequestParam String tenantCode,
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
        return workEffortCatalog.listDailyStatusActivitySummaries(
            tenantCode,
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalActorEmail(changedBy, "changedBy"),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toDailyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/daily-summary/by-current-status")
    public PageResult<DailyWorkEffortStatusActivityByCurrentStatusSummaryResponse> listDailyStatusActivityByCurrentStatusSummaries(
        @RequestParam String tenantCode,
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
        return workEffortCatalog.listDailyStatusActivityByCurrentStatusSummaries(
            tenantCode,
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalActorEmail(changedBy, "changedBy"),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toDailyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/status-activity/weekly-summary")
    public PageResult<WeeklyWorkEffortStatusActivitySummaryResponse> listWeeklyStatusActivitySummaries(
        @RequestParam String tenantCode,
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
        return workEffortCatalog.listWeeklyStatusActivitySummaries(
            tenantCode,
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalActorEmail(changedBy, "changedBy"),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toWeeklyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/weekly-summary/by-current-status")
    public PageResult<WeeklyWorkEffortStatusActivityByCurrentStatusSummaryResponse> listWeeklyStatusActivityByCurrentStatusSummaries(
        @RequestParam String tenantCode,
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
        return workEffortCatalog.listWeeklyStatusActivityByCurrentStatusSummaries(
            tenantCode,
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalActorEmail(changedBy, "changedBy"),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toWeeklyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/status-activity/monthly-summary")
    public PageResult<MonthlyWorkEffortStatusActivitySummaryResponse> listMonthlyStatusActivitySummaries(
        @RequestParam String tenantCode,
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
        return workEffortCatalog.listMonthlyStatusActivitySummaries(
            tenantCode,
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalActorEmail(changedBy, "changedBy"),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMonthlyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/monthly-summary/by-current-status")
    public PageResult<MonthlyWorkEffortStatusActivityByCurrentStatusSummaryResponse> listMonthlyStatusActivityByCurrentStatusSummaries(
        @RequestParam String tenantCode,
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
        return workEffortCatalog.listMonthlyStatusActivityByCurrentStatusSummaries(
            tenantCode,
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalActorEmail(changedBy, "changedBy"),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMonthlyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/{effortNumber}/status-history")
    public PageResult<WorkEffortStatusChangeResponse> listStatusHistory(
        @PathVariable String effortNumber,
        @RequestParam String tenantCode,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return workEffortCatalog.listStatusHistory(
            tenantCode,
            effortNumber,
            normalizeOptionalAssignedTo(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toStatusChangeResponse);
    }

    @GetMapping("/{effortNumber}/assignment-history")
    public PageResult<WorkEffortAssignmentChangeResponse> listAssignmentHistory(
        @PathVariable String effortNumber,
        @RequestParam String tenantCode,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedBy,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return workEffortCatalog.listAssignmentHistory(
            tenantCode,
            effortNumber,
            normalizeOptionalAssignedTo(assignedTo),
            normalizeOptionalActorEmail(assignedBy, "assignedBy"),
            parsedAssignedAtFrom,
            parsedAssignedAtTo,
            PageQuery.of(page, size)
        ).map(this::toAssignmentChangeResponse);
    }

    private WorkEffortResponse toResponse(WorkEffortView view) {
        return new WorkEffortResponse(
            view.id(),
            view.tenantCode(),
            view.effortNumber(),
            view.name(),
            view.description(),
            view.status(),
            view.assignedTo(),
            view.dueAt(),
            view.createdAt()
        );
    }

    private WorkEffortStatusChangeResponse toStatusChangeResponse(WorkEffortStatusChangeView view) {
        return new WorkEffortStatusChangeResponse(
            view.id(),
            view.effortNumber(),
            view.previousStatus(),
            view.currentStatus(),
            view.tenantCode(),
            view.reason(),
            view.changedBy(),
            view.changedAt()
        );
    }

    private WorkEffortAssignmentChangeResponse toAssignmentChangeResponse(WorkEffortAssignmentChangeView view) {
        return new WorkEffortAssignmentChangeResponse(
            view.id(),
            view.effortNumber(),
            view.previousAssignedTo(),
            view.currentAssignedTo(),
            view.tenantCode(),
            view.reason(),
            view.assignedBy(),
            view.assignedAt()
        );
    }

    private WorkEffortAssignmentSummaryResponse toAssignmentSummaryResponse(WorkEffortAssignmentSummaryView view) {
        return new WorkEffortAssignmentSummaryResponse(
            view.id(),
            view.tenantCode(),
            view.effortNumber(),
            view.assignedTo()
        );
    }

    private WorkEffortAssignmentActivitySummaryResponse toAssignmentActivitySummaryResponse(
        WorkEffortAssignmentActivitySummaryView view
    ) {
        return new WorkEffortAssignmentActivitySummaryResponse(
            view.tenantCode(),
            view.assignedTo(),
            view.assignmentCount(),
            view.firstAssignedAt(),
            view.lastAssignedAt()
        );
    }

    private DailyWorkEffortAssignmentActivitySummaryResponse toDailyAssignmentActivitySummaryResponse(
        DailyWorkEffortAssignmentActivitySummaryView view
    ) {
        return new DailyWorkEffortAssignmentActivitySummaryResponse(
            view.tenantCode(),
            view.businessDate(),
            view.assignmentCount(),
            view.workEffortCount()
        );
    }

    private WeeklyWorkEffortAssignmentActivitySummaryResponse toWeeklyAssignmentActivitySummaryResponse(
        WeeklyWorkEffortAssignmentActivitySummaryView view
    ) {
        return new WeeklyWorkEffortAssignmentActivitySummaryResponse(
            view.tenantCode(),
            view.businessWeekStart(),
            view.assignmentCount(),
            view.workEffortCount()
        );
    }

    private MonthlyWorkEffortAssignmentActivitySummaryResponse toMonthlyAssignmentActivitySummaryResponse(
        MonthlyWorkEffortAssignmentActivitySummaryView view
    ) {
        return new MonthlyWorkEffortAssignmentActivitySummaryResponse(
            view.tenantCode(),
            view.businessMonth(),
            view.assignmentCount(),
            view.workEffortCount()
        );
    }

    private DailyWorkEffortStatusActivitySummaryResponse toDailyStatusActivitySummaryResponse(
        DailyWorkEffortStatusActivitySummaryView view
    ) {
        return new DailyWorkEffortStatusActivitySummaryResponse(
            view.tenantCode(),
            view.businessDate(),
            view.transitionCount(),
            view.workEffortCount()
        );
    }

    private WeeklyWorkEffortStatusActivitySummaryResponse toWeeklyStatusActivitySummaryResponse(
        WeeklyWorkEffortStatusActivitySummaryView view
    ) {
        return new WeeklyWorkEffortStatusActivitySummaryResponse(
            view.tenantCode(),
            view.businessWeekStart(),
            view.transitionCount(),
            view.workEffortCount()
        );
    }

    private DailyWorkEffortStatusActivityByCurrentStatusSummaryResponse toDailyStatusActivityByCurrentStatusSummaryResponse(
        DailyWorkEffortStatusActivityByCurrentStatusSummaryView view
    ) {
        return new DailyWorkEffortStatusActivityByCurrentStatusSummaryResponse(
            view.tenantCode(),
            view.businessDate(),
            view.currentStatus(),
            view.transitionCount(),
            view.workEffortCount()
        );
    }

    private WeeklyWorkEffortStatusActivityByCurrentStatusSummaryResponse toWeeklyStatusActivityByCurrentStatusSummaryResponse(
        WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView view
    ) {
        return new WeeklyWorkEffortStatusActivityByCurrentStatusSummaryResponse(
            view.tenantCode(),
            view.businessWeekStart(),
            view.currentStatus(),
            view.transitionCount(),
            view.workEffortCount()
        );
    }

    private MonthlyWorkEffortStatusActivitySummaryResponse toMonthlyStatusActivitySummaryResponse(
        MonthlyWorkEffortStatusActivitySummaryView view
    ) {
        return new MonthlyWorkEffortStatusActivitySummaryResponse(
            view.tenantCode(),
            view.businessMonth(),
            view.transitionCount(),
            view.workEffortCount()
        );
    }

    private MonthlyWorkEffortStatusActivityByCurrentStatusSummaryResponse toMonthlyStatusActivityByCurrentStatusSummaryResponse(
        MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView view
    ) {
        return new MonthlyWorkEffortStatusActivityByCurrentStatusSummaryResponse(
            view.tenantCode(),
            view.businessMonth(),
            view.currentStatus(),
            view.transitionCount(),
            view.workEffortCount()
        );
    }

    private static WorkEffortStatus parseOptionalStatus(String status, String parameterName) {
        if (status == null) {
            return null;
        }
        if (status.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return WorkEffortStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be one of: PLANNED, IN_PROGRESS, COMPLETED");
        }
    }

    private static String normalizeOptionalAssignedTo(String assignedTo) {
        if (assignedTo == null) {
            return null;
        }
        if (assignedTo.isBlank()) {
            throw new IllegalArgumentException("assignedTo query parameter must not be blank");
        }
        return assignedTo.trim().toLowerCase();
    }

    private static String normalizeOptionalActorEmail(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        return value.trim().toLowerCase();
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
        validateInstantRange(changedAtFrom, changedAtTo, "changedAtFrom", "changedAtTo");
    }

    private static void validateInstantRange(Instant from, Instant to, String fromParameterName, String toParameterName) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(fromParameterName + " must be before or equal to " + toParameterName);
        }
    }
}
