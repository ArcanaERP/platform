package com.arcanaerp.platform.communicationevents.web;

import com.arcanaerp.platform.communicationevents.CommunicationEventPurposeTypeDirectory;
import com.arcanaerp.platform.communicationevents.CommunicationEventPurposeTypeView;
import com.arcanaerp.platform.communicationevents.CommunicationEventStatusTypeDirectory;
import com.arcanaerp.platform.communicationevents.CommunicationEventStatusTypeView;
import com.arcanaerp.platform.communicationevents.RegisterCommunicationEventPurposeTypeCommand;
import com.arcanaerp.platform.communicationevents.RegisterCommunicationEventStatusTypeCommand;
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
public class CommunicationEventReferenceDataController {

    private final CommunicationEventStatusTypeDirectory statusTypeDirectory;
    private final CommunicationEventPurposeTypeDirectory purposeTypeDirectory;

    @PostMapping("/status-types")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationEventStatusTypeResponse createStatusType(
        @Valid @RequestBody CreateCommunicationEventStatusTypeRequest request
    ) {
        CommunicationEventStatusTypeView created = statusTypeDirectory.registerStatusType(
            new RegisterCommunicationEventStatusTypeCommand(request.tenantCode(), request.code(), request.name())
        );
        return toStatusTypeResponse(created);
    }

    @GetMapping("/status-types/{code}")
    public CommunicationEventStatusTypeResponse statusTypeByCode(
        @PathVariable String code,
        @RequestParam String tenantCode
    ) {
        return toStatusTypeResponse(statusTypeDirectory.statusTypeByCode(tenantCode, code));
    }

    @GetMapping("/status-types")
    public PageResult<CommunicationEventStatusTypeResponse> listStatusTypes(
        @RequestParam String tenantCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return statusTypeDirectory.listStatusTypes(tenantCode, PageQuery.of(page, size)).map(this::toStatusTypeResponse);
    }

    @PostMapping("/purpose-types")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationEventPurposeTypeResponse createPurposeType(
        @Valid @RequestBody CreateCommunicationEventPurposeTypeRequest request
    ) {
        CommunicationEventPurposeTypeView created = purposeTypeDirectory.registerPurposeType(
            new RegisterCommunicationEventPurposeTypeCommand(request.tenantCode(), request.code(), request.name())
        );
        return toPurposeTypeResponse(created);
    }

    @GetMapping("/purpose-types/{code}")
    public CommunicationEventPurposeTypeResponse purposeTypeByCode(
        @PathVariable String code,
        @RequestParam String tenantCode
    ) {
        return toPurposeTypeResponse(purposeTypeDirectory.purposeTypeByCode(tenantCode, code));
    }

    @GetMapping("/purpose-types")
    public PageResult<CommunicationEventPurposeTypeResponse> listPurposeTypes(
        @RequestParam String tenantCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return purposeTypeDirectory.listPurposeTypes(tenantCode, PageQuery.of(page, size)).map(this::toPurposeTypeResponse);
    }

    private CommunicationEventStatusTypeResponse toStatusTypeResponse(CommunicationEventStatusTypeView view) {
        return new CommunicationEventStatusTypeResponse(
            view.id(),
            view.tenantCode(),
            view.code(),
            view.name(),
            view.createdAt()
        );
    }

    private CommunicationEventPurposeTypeResponse toPurposeTypeResponse(CommunicationEventPurposeTypeView view) {
        return new CommunicationEventPurposeTypeResponse(
            view.id(),
            view.tenantCode(),
            view.code(),
            view.name(),
            view.createdAt()
        );
    }
}
