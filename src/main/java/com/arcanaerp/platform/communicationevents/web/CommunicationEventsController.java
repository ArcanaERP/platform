package com.arcanaerp.platform.communicationevents.web;

import com.arcanaerp.platform.communicationevents.CommunicationEventLog;
import com.arcanaerp.platform.communicationevents.CommunicationEventView;
import com.arcanaerp.platform.communicationevents.CreateCommunicationEventCommand;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
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
        @RequestParam(required = false) String channel,
        @RequestParam(required = false) String direction,
        @RequestParam(required = false) String recordedBy,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return communicationEventLog.listEvents(
            tenantCode,
            PageQuery.of(page, size),
            channel,
            direction,
            recordedBy
        ).map(this::toResponse);
    }

    private CommunicationEventResponse toResponse(CommunicationEventView event) {
        return new CommunicationEventResponse(
            event.id(),
            event.eventNumber(),
            event.tenantCode(),
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
}
