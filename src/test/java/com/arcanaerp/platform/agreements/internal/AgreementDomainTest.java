package com.arcanaerp.platform.agreements.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AgreementDomainTest {

    @Test
    void createNormalizesAgreementFields() {
        Agreement agreement = Agreement.create(
            "  agr-1000 ",
            "  Master Supply Agreement ",
            " purchase ",
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-01T01:00:00Z")
        );

        assertThat(agreement.getAgreementNumber()).isEqualTo("AGR-1000");
        assertThat(agreement.getName()).isEqualTo("Master Supply Agreement");
        assertThat(agreement.getAgreementType()).isEqualTo("PURCHASE");
        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.DRAFT);
    }

    @Test
    void createRejectsMissingEffectiveFrom() {
        assertThatThrownBy(() ->
            Agreement.create(
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
}
