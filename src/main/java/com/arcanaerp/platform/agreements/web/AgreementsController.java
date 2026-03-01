package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementManagement;
import com.arcanaerp.platform.agreements.AgreementView;
import com.arcanaerp.platform.agreements.ChangeAgreementStatusCommand;
import com.arcanaerp.platform.agreements.CreateAgreementCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PatchMapping("/{agreementNumber}/status")
    public AgreementResponse changeAgreementStatus(
        @PathVariable String agreementNumber,
        @Valid @RequestBody ChangeAgreementStatusRequest request
    ) {
        AgreementView agreement = agreementManagement.changeAgreementStatus(
            new ChangeAgreementStatusCommand(agreementNumber, request.status())
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
}
