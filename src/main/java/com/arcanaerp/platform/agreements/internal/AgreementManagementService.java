package com.arcanaerp.platform.agreements.internal;

import com.arcanaerp.platform.agreements.AgreementManagement;
import com.arcanaerp.platform.agreements.AgreementStatus;
import com.arcanaerp.platform.agreements.AgreementStatusChangeView;
import com.arcanaerp.platform.agreements.AgreementView;
import com.arcanaerp.platform.agreements.ChangeAgreementStatusCommand;
import com.arcanaerp.platform.agreements.CreateAgreementCommand;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class AgreementManagementService implements AgreementManagement {

    private final AgreementRepository agreementRepository;
    private final AgreementStatusChangeAuditRepository agreementStatusChangeAuditRepository;
    private final IdentityActorLookup identityActorLookup;
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
        String tenantCode = normalizeTenantCode(command.tenantCode());
        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeRequired(command.changedBy(), "changedBy").toLowerCase();

        Agreement agreement = agreementRepository.findByAgreementNumber(normalizedAgreementNumber)
            .orElseThrow(() -> new java.util.NoSuchElementException("Agreement not found: " + normalizedAgreementNumber));

        if (!identityActorLookup.actorExists(tenantCode, changedBy)) {
            throw new IllegalArgumentException(
                "Agreement status actor not found in tenant " + tenantCode + ": " + changedBy
            );
        }

        AgreementStatus previousStatus = agreement.getStatus();
        Instant changedAt = Instant.now(clock);
        agreement.transitionTo(targetStatus, changedAt);
        Agreement saved = agreementRepository.save(agreement);
        if (previousStatus != saved.getStatus()) {
            agreementStatusChangeAuditRepository.save(
                AgreementStatusChangeAudit.create(
                    saved.getId(),
                    previousStatus,
                    saved.getStatus(),
                    tenantCode,
                    reason,
                    changedBy,
                    changedAt
                )
            );
        }
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementView getAgreement(String agreementNumber) {
        return toView(findAgreementByNumber(agreementNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgreementView> listAgreements(PageQuery pageQuery, AgreementStatus status) {
        Page<Agreement> agreements = status == null
            ? agreementRepository.findAll(pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt")))
            : agreementRepository.findByStatus(status, pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.from(agreements).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgreementStatusChangeView> listStatusHistory(String agreementNumber, PageQuery pageQuery) {
        Agreement agreement = findAgreementByNumber(agreementNumber);
        Page<AgreementStatusChangeAudit> audits = agreementStatusChangeAuditRepository.findByAgreementId(
            agreement.getId(),
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(audits).map(audit -> new AgreementStatusChangeView(
                audit.getId(),
                agreement.getAgreementNumber(),
                audit.getPreviousStatus(),
                audit.getCurrentStatus(),
                audit.getTenantCode(),
                audit.getReason(),
                audit.getChangedBy(),
                audit.getChangedAt()
            ));
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeTenantCode(String tenantCode) {
        return normalizeRequired(tenantCode, "tenantCode").toUpperCase();
    }

    private Agreement findAgreementByNumber(String agreementNumber) {
        String normalizedAgreementNumber = normalizeRequired(agreementNumber, "agreementNumber").toUpperCase();
        return agreementRepository.findByAgreementNumber(normalizedAgreementNumber)
            .orElseThrow(() -> new java.util.NoSuchElementException("Agreement not found: " + normalizedAgreementNumber));
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
