package com.arcanaerp.platform.rules.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RulesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsReadsAndListsRuleDefinitions() throws Exception {
        RulesWebIntegrationTestSupport.createRuleDefinition(
            mockMvc,
            "rulesweb01",
            "fraud_hold",
            "Fraud Hold",
            "orders",
            "total > 1000",
            "Review high-risk orders",
            true
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("RULESWEB01"))
            .andExpect(jsonPath("$.code").value("FRAUD_HOLD"))
            .andExpect(jsonPath("$.appliesTo").value("ORDERS"))
            .andExpect(jsonPath("$.active").value(true));

        RulesWebIntegrationTestSupport.createRuleDefinition(
            mockMvc,
            "rulesweb01",
            "late_fee_notice",
            "Late Fee Notice",
            "invoices",
            "daysPastDue >= 30",
            null,
            false
        )
            .andExpect(status().isCreated());

        mockMvc.perform(RulesWebIntegrationTestSupport.getRuleDefinitionRequest("rulesweb01", "fraud_hold"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("FRAUD_HOLD"))
            .andExpect(jsonPath("$.name").value("Fraud Hold"));

        mockMvc.perform(
            RulesWebIntegrationTestSupport.listRuleDefinitionsRequest(
                "rulesweb01",
                0,
                10,
                "active", "true",
                "appliesTo", "orders"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.code=='FRAUD_HOLD')].name", hasItem("Fraud Hold")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        RulesWebIntegrationTestSupport.createRuleDefinition(
            mockMvc,
            "rulesweb02",
            "credit_hold",
            "Credit Hold",
            "orders",
            "creditScore < 50",
            null,
            true
        )
            .andExpect(status().isCreated());

        mockMvc.perform(RulesWebIntegrationTestSupport.listRuleDefinitionsRequest("rulesweb02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.code=='CREDIT_HOLD')].name", hasItem("Credit Hold")));
    }

    @Test
    void rejectsDuplicateTenantLocalRuleCodes() throws Exception {
        RulesWebIntegrationTestSupport.createRuleDefinition(
            mockMvc,
            "rulesweb03",
            "fraud_hold",
            "Fraud Hold",
            "orders",
            "total > 1000",
            null,
            true
        )
            .andExpect(status().isCreated());

        RulesWebIntegrationTestSupport.createRuleDefinition(
            mockMvc,
            "rulesweb03",
            "FRAUD_HOLD",
            "Duplicate Fraud Hold",
            "orders",
            "total > 5000",
            null,
            true
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Rule definition already exists for tenant/code: RULESWEB03/FRAUD_HOLD"))
            .andExpect(jsonPath("$.path").value("/api/rules"));
    }

    @Test
    void returnsNotFoundForMissingRuleDefinition() throws Exception {
        mockMvc.perform(RulesWebIntegrationTestSupport.getRuleDefinitionRequest("rulesweb04", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Rule definition not found for tenant/code: RULESWEB04/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/rules/missing"));
    }

    @Test
    void rejectsInvalidFiltersAndPagination() throws Exception {
        mockMvc.perform(
            RulesWebIntegrationTestSupport.listRuleDefinitionsRequest("rulesweb05", 0, 10, "appliesTo", "   ")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("appliesTo query parameter must not be blank"));

        mockMvc.perform(RulesWebIntegrationTestSupport.listRuleDefinitionsRequest("rulesweb05", -1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"));
    }
}
