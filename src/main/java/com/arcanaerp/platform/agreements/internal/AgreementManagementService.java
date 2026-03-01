package com.arcanaerp.platform.agreements.internal;

import com.arcanaerp.platform.agreements.AgreementManagement;
import com.arcanaerp.platform.agreements.AgreementStatus;
import com.arcanaerp.platform.agreements.AgreementView;
import com.arcanaerp.platform.agreements.ChangeAgreementStatusCommand;
import com.arcanaerp.platform.agreements.CreateAgreementCommand;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class AgreementManagementService implements AgreementManagement {

    private final AgreementRepository agreementRepository;
    private final Clock clock;

    @Override
    public AgreementView createAgreement(CreateAgreementCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        String normalizedAgreementNumber = normalizeRequired(command.agreementNumber(), "agreementNumber").toUpperCase();
        if (agreementRepository.findByAgreementNumber(normalizedAgreementNumber).isPresent()) {
            throw new IllegalArgumentException("Agreement number already exists: " + normalizedAgreementNumber);
        }

        Agreement agreement = agreementRepository.save(
            Agreement.create(
                normalizedAgreementNumber,
                command.name(),
                command.agreementType(),
                command.effectiveFrom(),
                Instant.now(clock)
            )
        );
        return toView(agreement);
    }

    @Override
    public AgreementView changeAgreementStatus(ChangeAgreementStatusCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        String normalizedAgreementNumber = normalizeRequired(command.agreementNumber(), "agreementNumber").toUpperCase();
        AgreementStatus targetStatus = command.status();
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }

        Agreement agreement = agreementRepository.findByAgreementNumber(normalizedAgreementNumber)
            .orElseThrow(() -> new java.util.NoSuchElementException("Agreement not found: " + normalizedAgreementNumber));

        agreement.transitionTo(targetStatus, Instant.now(clock));
        Agreement saved = agreementRepository.save(agreement);
        return toView(saved);
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private AgreementView toView(Agreement agreement) {
        return new AgreementView(
            agreement.getId(),
            agreement.getAgreementNumber(),
            agreement.getName(),
            agreement.getAgreementType(),
            agreement.getStatus(),
            agreement.getEffectiveFrom(),
            agreement.getCreatedAt(),
            agreement.getActivatedAt(),
            agreement.getTerminatedAt()
        );
    }
}
