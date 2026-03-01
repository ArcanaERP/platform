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
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        agreementStatusChangeAuditRepository.save(
            AgreementStatusChangeAudit.create(
                agreementId,
                AgreementStatus.DRAFT,
                AgreementStatus.ACTIVE,
                Instant.parse("2026-03-01T02:00:00Z")
            )
        );

        var page = agreementStatusChangeAuditRepository.findByAgreementId(
            agreementId,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "changedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getCurrentStatus()).isEqualTo(AgreementStatus.ACTIVE);
        assertThat(page.getContent().get(1).getCurrentStatus()).isEqualTo(AgreementStatus.TERMINATED);
    }
}
