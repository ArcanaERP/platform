package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementManagement;
import com.arcanaerp.platform.agreements.AgreementStatus;
import com.arcanaerp.platform.agreements.AgreementStatusChangeView;
import com.arcanaerp.platform.agreements.AgreementView;
import com.arcanaerp.platform.agreements.ChangeAgreementStatusCommand;
import com.arcanaerp.platform.agreements.CreateAgreementCommand;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import jakarta.validation.Valid;
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
        @RequestParam(required = false) String status
    ) {
        return agreementManagement.listAgreements(PageQuery.of(page, size), parseOptionalStatus(status)).map(this::toResponse);
    }

    @GetMapping("/{agreementNumber}/status-history")
    public PageResult<AgreementStatusChangeResponse> listStatusHistory(
        @PathVariable String agreementNumber,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return agreementManagement.listStatusHistory(agreementNumber, PageQuery.of(page, size)).map(this::toStatusHistoryResponse);
    }

    @PatchMapping("/{agreementNumber}/status")
    public AgreementResponse changeAgreementStatus(
        @PathVariable String agreementNumber,
        @Valid @RequestBody ChangeAgreementStatusRequest request
    ) {
        AgreementView agreement = agreementManagement.changeAgreementStatus(
            new ChangeAgreementStatusCommand(agreementNumber, request.status(), request.reason(), request.changedBy())
        );
        return toResponse(agreement);
    }

    private AgreementResponse toResponse(AgreementView agreement) {
        return new AgreementResponse(
                agreement.id(),
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
            change.reason(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private static AgreementStatus parseOptionalStatus(String status) {
        if (status == null) {
            return null;
        }
        if (status.isBlank()) {
            throw new IllegalArgumentException("status query parameter must not be blank");
        }
        String normalized = status.trim().toUpperCase();
        try {
            return AgreementStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("status query parameter must be one of: DRAFT, ACTIVE, TERMINATED");
        }
    }
}
