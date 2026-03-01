package com.arcanaerp.platform.agreements.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class AgreementStatusChangeAuditRepositoryTest {

    @Autowired
    private AgreementStatusChangeAuditRepository agreementStatusChangeAuditRepository;

    @Test
    void listsStatusChangesForAgreementOrderedByChangedAtDesc() {
        UUID agreementId = UUID.randomUUID();
        agreementStatusChangeAuditRepository.save(
            AgreementStatusChangeAudit.create(
                agreementId,
                AgreementStatus.DRAFT,
                AgreementStatus.TERMINATED,
                "TENANT02",
                "Contract breach",
                "ops@arcanaerp.com",
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        agreementStatusChangeAuditRepository.save(
            AgreementStatusChangeAudit.create(
                agreementId,
                AgreementStatus.DRAFT,
                AgreementStatus.ACTIVE,
                "TENANT01",
                "Initial activation",
                "legal@arcanaerp.com",
                Instant.parse("2026-03-01T02:00:00Z")
            )
        );

        var page = agreementStatusChangeAuditRepository.findByAgreementId(
            agreementId,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "changedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getCurrentStatus()).isEqualTo(AgreementStatus.ACTIVE);
        assertThat(page.getContent().get(0).getTenantCode()).isEqualTo("TENANT01");
        assertThat(page.getContent().get(0).getChangedBy()).isEqualTo("legal@arcanaerp.com");
        assertThat(page.getContent().get(0).getReason()).isEqualTo("Initial activation");
        assertThat(page.getContent().get(1).getCurrentStatus()).isEqualTo(AgreementStatus.TERMINATED);
    }

    @Test
    void filtersStatusHistoryByTenantChangedByAndChangedAtRange() {
        UUID agreementId = UUID.randomUUID();
        agreementStatusChangeAuditRepository.save(
            AgreementStatusChangeAudit.create(
                agreementId,
                AgreementStatus.DRAFT,
                AgreementStatus.ACTIVE,
                "TENANT01",
                "First activation",
                "actor.one@arcanaerp.com",
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        agreementStatusChangeAuditRepository.save(
            AgreementStatusChangeAudit.create(
                agreementId,
                AgreementStatus.DRAFT,
                AgreementStatus.TERMINATED,
                "TENANT02",
                "Termination",
                "actor.two@arcanaerp.com",
                Instant.parse("2026-03-01T02:00:00Z")
            )
        );

        var tenantFiltered = agreementStatusChangeAuditRepository.findHistoryFiltered(
            agreementId,
            "TENANT01",
            null,
            null,
            null,
            PageRequest.of(0, 10)
        );
        var actorFiltered = agreementStatusChangeAuditRepository.findHistoryFiltered(
            agreementId,
            null,
            "actor.two@arcanaerp.com",
            null,
            null,
            PageRequest.of(0, 10)
        );
        var rangeFiltered = agreementStatusChangeAuditRepository.findHistoryFiltered(
            agreementId,
            null,
            null,
            Instant.parse("2026-03-01T01:30:00Z"),
            Instant.parse("2026-03-01T02:30:00Z"),
            PageRequest.of(0, 10)
        );

        assertThat(tenantFiltered.getTotalElements()).isEqualTo(1);
        assertThat(tenantFiltered.getContent().get(0).getTenantCode()).isEqualTo("TENANT01");
        assertThat(actorFiltered.getTotalElements()).isEqualTo(1);
        assertThat(actorFiltered.getContent().get(0).getChangedBy()).isEqualTo("actor.two@arcanaerp.com");
        assertThat(rangeFiltered.getTotalElements()).isEqualTo(1);
        assertThat(rangeFiltered.getContent().get(0).getCurrentStatus()).isEqualTo(AgreementStatus.TERMINATED);
    }
}
