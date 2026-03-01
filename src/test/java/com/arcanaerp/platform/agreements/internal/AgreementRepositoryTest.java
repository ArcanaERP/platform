package com.arcanaerp.platform.agreements.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class AgreementRepositoryTest {

    @Autowired
    private AgreementRepository agreementRepository;

    @Test
    void findsAgreementByAgreementNumber() {
        agreementRepository.save(
            Agreement.create(
                "agr-2000",
                "Distribution Agreement",
                "SALES",
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );

        Agreement agreement = agreementRepository.findByAgreementNumber("AGR-2000").orElseThrow();

        assertThat(agreement.getAgreementNumber()).isEqualTo("AGR-2000");
        assertThat(agreement.getAgreementType()).isEqualTo("SALES");
    }

    @Test
    void enforcesUniqueAgreementNumber() {
        agreementRepository.saveAndFlush(
            Agreement.create(
                "agr-2001",
                "Services Agreement A",
                "SERVICE",
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );

        assertThatThrownBy(() ->
            agreementRepository.saveAndFlush(
                Agreement.create(
                    "AGR-2001",
                    "Services Agreement B",
                    "SERVICE",
                    Instant.parse("2026-03-01T00:00:00Z"),
                    Instant.parse("2026-03-01T01:00:00Z")
                )
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void filtersAgreementsByStatus() {
        Agreement draftAgreement = agreementRepository.saveAndFlush(
            Agreement.create(
                "agr-2002",
                "Draft Agreement",
                "SERVICE",
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        Agreement activeAgreement = agreementRepository.saveAndFlush(
            Agreement.create(
                "agr-2003",
                "Active Agreement",
                "SERVICE",
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-01T01:00:01Z")
            )
        );
        activeAgreement.transitionTo(AgreementStatus.ACTIVE, Instant.parse("2026-03-01T02:00:00Z"));
        agreementRepository.saveAndFlush(activeAgreement);

        var activePage = agreementRepository.findByStatus(
            AgreementStatus.ACTIVE,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        var draftPage = agreementRepository.findByStatus(
            AgreementStatus.DRAFT,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(activePage.getContent())
            .extracting(Agreement::getAgreementNumber)
            .contains("AGR-2003")
            .doesNotContain("AGR-2002");
        assertThat(draftPage.getContent())
            .extracting(Agreement::getAgreementNumber)
            .contains("AGR-2002")
            .doesNotContain("AGR-2003");
        assertThat(draftAgreement.getStatus()).isEqualTo(AgreementStatus.DRAFT);
    }
}
