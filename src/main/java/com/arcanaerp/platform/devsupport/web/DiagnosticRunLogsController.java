package com.arcanaerp.platform.devsupport.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.devsupport.DiagnosticRunLogView;
import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
import com.arcanaerp.platform.devsupport.DevSupportCatalog;
import com.arcanaerp.platform.devsupport.RegisterDiagnosticRunLogCommand;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
@RequestMapping("/api/dev-support/diagnostic-run-logs")
@RequiredArgsConstructor
public class DiagnosticRunLogsController {

    private final DevSupportCatalog devSupportCatalog;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosticRunLogResponse registerDiagnosticRunLog(@Valid @RequestBody CreateDiagnosticRunLogRequest request) {
        DiagnosticRunLogView created = devSupportCatalog.registerDiagnosticRunLog(
            new RegisterDiagnosticRunLogCommand(
                request.tenantCode(),
                request.runNumber(),
                request.diagnosticCode(),
                request.title(),
                request.summary(),
                request.status(),
                request.startedAt(),
                request.finishedAt()
            )
        );
        return toResponse(created);
    }

    @GetMapping("/{runNumber}")
    public DiagnosticRunLogResponse getDiagnosticRunLog(
        @PathVariable String runNumber,
        @RequestParam String tenantCode
    ) {
        return toResponse(devSupportCatalog.getDiagnosticRunLog(tenantCode, runNumber));
    }

    @GetMapping
    public PageResult<DiagnosticRunLogResponse> listDiagnosticRunLogs(
        @RequestParam String tenantCode,
        @RequestParam(required = false) DiagnosticRunStatus status,
        @RequestParam(required = false) String startedAtFrom,
        @RequestParam(required = false) String startedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedStartedAtFrom = parseOptionalInstant(startedAtFrom, "startedAtFrom");
        Instant parsedStartedAtTo = parseOptionalInstant(startedAtTo, "startedAtTo");
        validateStartedAtRange(parsedStartedAtFrom, parsedStartedAtTo);

        return devSupportCatalog.listDiagnosticRunLogs(
            tenantCode,
            PageQuery.of(page, size),
            status,
            parsedStartedAtFrom,
            parsedStartedAtTo
        ).map(this::toResponse);
    }

    private DiagnosticRunLogResponse toResponse(DiagnosticRunLogView view) {
        return new DiagnosticRunLogResponse(
            view.id(),
            view.tenantCode(),
            view.runNumber(),
            view.diagnosticCode(),
            view.title(),
            view.summary(),
            view.status(),
            view.startedAt(),
            view.finishedAt(),
            view.createdAt()
        );
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

    private static void validateStartedAtRange(Instant startedAtFrom, Instant startedAtTo) {
        if (startedAtFrom != null && startedAtTo != null && startedAtFrom.isAfter(startedAtTo)) {
            throw new IllegalArgumentException("startedAtFrom must be before or equal to startedAtTo");
        }
    }
}
