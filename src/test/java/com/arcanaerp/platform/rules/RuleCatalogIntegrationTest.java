package com.arcanaerp.platform.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RuleCatalogIntegrationTest {

    @Autowired
    private RuleCatalog ruleCatalog;

    @Test
    void registersReadsAndListsRuleDefinitions() {
        RuleDefinitionView created = ruleCatalog.registerRuleDefinition(
            new RegisterRuleDefinitionCommand(
                "rules01",
                "fraud_hold",
                "Fraud Hold",
                "orders",
                "total > 1000",
                "Review high-risk orders",
                true
            )
        );
        ruleCatalog.registerRuleDefinition(
            new RegisterRuleDefinitionCommand(
                "rules01",
                "late_fee_notice",
                "Late Fee Notice",
                "invoices",
                "daysPastDue >= 30",
                "Notify billing",
                false
            )
        );

        RuleDefinitionView loaded = ruleCatalog.getRuleDefinition("rules01", "fraud_hold");
        var listed = ruleCatalog.listRuleDefinitions("rules01", new PageQuery(0, 10), true, "orders");

        assertThat(loaded.code()).isEqualTo(created.code());
        assertThat(loaded.tenantCode()).isEqualTo("RULES01");
        assertThat(loaded.appliesTo()).isEqualTo("ORDERS");
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(RuleDefinitionView::code).containsExactly("FRAUD_HOLD");
    }

    @Test
    void rejectsDuplicateTenantLocalRuleCodes() {
        ruleCatalog.registerRuleDefinition(
            new RegisterRuleDefinitionCommand(
                "rules02",
                "fraud_hold",
                "Fraud Hold",
                "orders",
                "total > 1000",
                null,
                true
            )
        );

        assertThatThrownBy(() -> ruleCatalog.registerRuleDefinition(
            new RegisterRuleDefinitionCommand(
                "rules02",
                "FRAUD_HOLD",
                "Duplicate Fraud Hold",
                "orders",
                "total > 5000",
                null,
                true
            )
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Rule definition already exists for tenant/code: RULES02/FRAUD_HOLD");
    }

    @Test
    void rejectsMissingRuleLookup() {
        assertThatThrownBy(() -> ruleCatalog.getRuleDefinition("rules03", "missing"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Rule definition not found for tenant/code: RULES03/MISSING");
    }
}
