package com.arcanaerp.platform.agreements.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgreementDomainTest {

    @Test
    void createNormalizesAgreementFields() {
        Agreement agreement = Agreement.create(
            "tenant-agreements",
            "  agr-1000 ",
            "  Master Supply Agreement ",
            " purchase ",
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-01T01:00:00Z")
        );

        assertThat(agreement.getTenantCode()).isEqualTo("TENANT-AGREEMENTS");
        assertThat(agreement.getAgreementNumber()).isEqualTo("AGR-1000");
        assertThat(agreement.getName()).isEqualTo("Master Supply Agreement");
        assertThat(agreement.getAgreementType()).isEqualTo("PURCHASE");
        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.DRAFT);
    }

    @Test
    void createPreservesLegacyAgreementMasterFields() {
        UUID agreementTypeId = UUID.randomUUID();
        Agreement agreement = Agreement.create(
            "tenant-agreements",
            "agr-legacy-1",
            "Legacy Agreement",
            "service",
            Instant.parse("2026-03-01T00:00:00Z"),
            "  Legacy description ",
            agreementTypeId,
            " pending review ",
            42L,
            LocalDate.parse("2026-02-01"),
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2027-03-01"),
            " EXT-AGR-1 ",
            " legacy ",
            Instant.parse("2026-03-01T01:00:00Z")
        );

        assertThat(agreement.getDescription()).isEqualTo("Legacy description");
        assertThat(agreement.getAgreementTypeId()).isEqualTo(agreementTypeId);
        assertThat(agreement.getAgreementStatus()).isEqualTo("pending review");
        assertThat(agreement.getProductId()).isEqualTo(42L);
        assertThat(agreement.getAgreementDate()).isEqualTo(LocalDate.parse("2026-02-01"));
        assertThat(agreement.getFromDate()).isEqualTo(LocalDate.parse("2026-03-01"));
        assertThat(agreement.getThruDate()).isEqualTo(LocalDate.parse("2027-03-01"));
        assertThat(agreement.getExternalIdentifier()).isEqualTo("EXT-AGR-1");
        assertThat(agreement.getExternalIdSource()).isEqualTo("legacy");
        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.DRAFT);
    }

    @Test
    void createRejectsMissingEffectiveFrom() {
        assertThatThrownBy(() ->
            Agreement.create(
                "tenant-agreements",
                "AGR-1001",
                "Master Supply Agreement",
                "PURCHASE",
                null,
                Instant.parse("2026-03-01T01:00:00Z")
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("effectiveFrom is required");
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() ->
            Agreement.create(
                "tenant-agreements",
                "AGR-1002",
                "   ",
                "PURCHASE",
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-01T01:00:00Z")
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("name is required");
    }

    @Test
    void transitionAllowsDraftToActive() {
        Agreement agreement = Agreement.create(
            "tenant-agreements",
            "AGR-1003",
            "Master Supply Agreement",
            "PURCHASE",
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-01T01:00:00Z")
        );

        Instant activatedAt = Instant.parse("2026-03-01T02:00:00Z");
        agreement.transitionTo(AgreementStatus.ACTIVE, activatedAt);

        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.ACTIVE);
        assertThat(agreement.getActivatedAt()).isEqualTo(activatedAt);
        assertThat(agreement.getTerminatedAt()).isNull();
    }

    @Test
    void transitionAllowsActiveToTerminated() {
        Agreement agreement = Agreement.create(
            "tenant-agreements",
            "AGR-1004",
            "Master Supply Agreement",
            "PURCHASE",
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-01T01:00:00Z")
        );
        Instant activatedAt = Instant.parse("2026-03-01T02:00:00Z");
        Instant terminatedAt = Instant.parse("2026-03-01T03:00:00Z");
        agreement.transitionTo(AgreementStatus.ACTIVE, activatedAt);
        agreement.transitionTo(AgreementStatus.TERMINATED, terminatedAt);

        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.TERMINATED);
        assertThat(agreement.getActivatedAt()).isEqualTo(activatedAt);
        assertThat(agreement.getTerminatedAt()).isEqualTo(terminatedAt);
    }

    @Test
    void transitionRejectsTerminatedToActive() {
        Agreement agreement = Agreement.create(
            "tenant-agreements",
            "AGR-1005",
            "Master Supply Agreement",
            "PURCHASE",
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-01T01:00:00Z")
        );
        agreement.transitionTo(AgreementStatus.TERMINATED, Instant.parse("2026-03-01T02:00:00Z"));

        assertThatThrownBy(() ->
            agreement.transitionTo(AgreementStatus.ACTIVE, Instant.parse("2026-03-01T03:00:00Z"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Agreement status transition not allowed: TERMINATED -> ACTIVE");
    }
}
