package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.workeffort.CreateWorkEffortCommand;
import com.arcanaerp.platform.workeffort.WorkEffortCatalog;
import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import com.arcanaerp.platform.workeffort.WorkEffortView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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

    private static String normalizeOptionalAssignedTo(String assignedTo) {
        if (assignedTo == null) {
            return null;
        }
        if (assignedTo.isBlank()) {
            throw new IllegalArgumentException("assignedTo query parameter must not be blank");
        }
        return assignedTo.trim().toLowerCase();
    }
}
