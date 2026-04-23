package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.workeffort.ChangeWorkEffortStatusCommand;
import com.arcanaerp.platform.workeffort.CreateWorkEffortCommand;
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

    private static String normalizeOptionalAssignedTo(String assignedTo) {
        if (assignedTo == null) {
            return null;
        }
        if (assignedTo.isBlank()) {
            throw new IllegalArgumentException("assignedTo query parameter must not be blank");
        }
        return assignedTo.trim().toLowerCase();
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
