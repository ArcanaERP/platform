package com.arcanaerp.platform.communicationevents.web;

import com.arcanaerp.platform.communicationevents.ChangeCommunicationEventStatusCommand;
import com.arcanaerp.platform.communicationevents.CommunicationEventLog;
import com.arcanaerp.platform.communicationevents.CommunicationEventStatusChangeView;
import com.arcanaerp.platform.communicationevents.CommunicationEventView;
import com.arcanaerp.platform.communicationevents.CreateCommunicationEventCommand;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
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
@RequestMapping("/api/communication-events")
@RequiredArgsConstructor
public class CommunicationEventsController {

    private final CommunicationEventLog communicationEventLog;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationEventResponse createEvent(@Valid @RequestBody CreateCommunicationEventRequest request) {
        CommunicationEventView created = communicationEventLog.createEvent(
            new CreateCommunicationEventCommand(
                request.tenantCode(),
                request.statusCode(),
                request.purposeCode(),
                request.channel(),
                request.direction(),
                request.subject(),
                request.summary(),
                request.occurredAt(),
                request.recordedBy(),
                request.externalReference()
            )
        );
        return toResponse(created);
    }

    @PatchMapping("/{eventNumber}/status")
    public CommunicationEventResponse changeStatus(
        @PathVariable String eventNumber,
        @Valid @RequestBody ChangeCommunicationEventStatusRequest request
    ) {
        return toResponse(communicationEventLog.changeStatus(
            new ChangeCommunicationEventStatusCommand(
                request.tenantCode(),
                eventNumber,
                request.statusCode(),
                request.reason(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{eventNumber}")
    public CommunicationEventResponse getEvent(
        @PathVariable String eventNumber,
        @RequestParam String tenantCode
    ) {
        return toResponse(communicationEventLog.getEvent(tenantCode, eventNumber));
    }

    @GetMapping
    public PageResult<CommunicationEventResponse> listEvents(
        @RequestParam String tenantCode,
        @RequestParam(required = false) String statusCode,
        @RequestParam(required = false) String purposeCode,
        @RequestParam(required = false) String channel,
        @RequestParam(required = false) String direction,
        @RequestParam(required = false) String recordedBy,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return communicationEventLog.listEvents(
            tenantCode,
            PageQuery.of(page, size),
            statusCode,
            purposeCode,
            channel,
            direction,
            recordedBy
        ).map(this::toResponse);
    }

    @GetMapping("/{eventNumber}/status-history")
    public PageResult<CommunicationEventStatusChangeResponse> listStatusHistory(
        @PathVariable String eventNumber,
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
        return communicationEventLog.listStatusHistory(
            tenantCode,
            eventNumber,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toStatusChangeResponse);
    }

    private CommunicationEventResponse toResponse(CommunicationEventView event) {
        return new CommunicationEventResponse(
            event.id(),
            event.eventNumber(),
            event.tenantCode(),
            event.statusCode(),
            event.statusName(),
            event.purposeCode(),
            event.purposeName(),
            event.channel(),
            event.direction(),
            event.subject(),
            event.summary(),
            event.occurredAt(),
            event.recordedBy(),
            event.externalReference(),
            event.createdAt()
        );
    }

    private CommunicationEventStatusChangeResponse toStatusChangeResponse(CommunicationEventStatusChangeView view) {
        return new CommunicationEventStatusChangeResponse(
            view.id(),
            view.eventNumber(),
            view.previousStatusCode(),
            view.previousStatusName(),
            view.currentStatusCode(),
            view.currentStatusName(),
            view.tenantCode(),
            view.reason(),
            view.changedBy(),
            view.changedAt()
        );
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
