package com.arcanaerp.platform.rules.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RuleDefinitionDomainTest {

    @Test
    void normalizesRuleDefinitionFields() {
        RuleDefinition rule = RuleDefinition.create(
            "tenant01",
            "fraud_hold",
            "Fraud Hold",
            "orders",
            "total > 1000",
            "Pause review",
            true,
            Instant.parse("2026-04-18T12:00:00Z")
        );

        assertThat(rule.getTenantCode()).isEqualTo("TENANT01");
        assertThat(rule.getCode()).isEqualTo("FRAUD_HOLD");
        assertThat(rule.getAppliesTo()).isEqualTo("ORDERS");
        assertThat(rule.getDescription()).isEqualTo("Pause review");
        assertThat(rule.isActive()).isTrue();
    }

    @Test
    void rejectsMissingExpression() {
        assertThatThrownBy(() -> RuleDefinition.create(
            "tenant01",
            "fraud_hold",
            "Fraud Hold",
            "orders",
            "   ",
            null,
            true,
            Instant.parse("2026-04-18T12:00:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("expression is required");
    }
}
