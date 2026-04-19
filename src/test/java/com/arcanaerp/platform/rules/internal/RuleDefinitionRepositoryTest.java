package com.arcanaerp.platform.rules.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class RuleDefinitionRepositoryTest {

    @Autowired
    private RuleDefinitionRepository ruleDefinitionRepository;

    @Test
    void filtersRuleDefinitionsByTenantActiveAndAppliesTo() {
        ruleDefinitionRepository.save(
            RuleDefinition.create(
                "tenant01",
                "fraud_hold",
                "Fraud Hold",
                "orders",
                "total > 1000",
                "High-risk order review",
                true,
                Instant.parse("2026-04-18T10:00:00Z")
            )
        );
        ruleDefinitionRepository.save(
            RuleDefinition.create(
                "tenant01",
                "late_fee_notice",
                "Late Fee Notice",
                "invoices",
                "daysPastDue >= 30",
                "Notify billing",
                false,
                Instant.parse("2026-04-18T11:00:00Z")
            )
        );
        ruleDefinitionRepository.save(
            RuleDefinition.create(
                "tenant02",
                "credit_hold",
                "Credit Hold",
                "orders",
                "creditScore < 50",
                null,
                true,
                Instant.parse("2026-04-18T12:00:00Z")
            )
        );

        var activeOrders = ruleDefinitionRepository.findByTenantCodeAndActiveAndAppliesTo(
            "TENANT01",
            true,
            "ORDERS",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        var tenantOnly = ruleDefinitionRepository.findByTenantCode(
            "TENANT01",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(activeOrders.getTotalElements()).isEqualTo(1);
        assertThat(activeOrders.getContent().get(0).getCode()).isEqualTo("FRAUD_HOLD");
        assertThat(tenantOnly.getTotalElements()).isEqualTo(2);
        assertThat(tenantOnly.getContent()).extracting(RuleDefinition::getCode)
            .containsExactly("LATE_FEE_NOTICE", "FRAUD_HOLD");
    }
}
