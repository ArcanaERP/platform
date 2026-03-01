package com.arcanaerp.platform.products.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
import com.arcanaerp.platform.testsupport.web.ProductCatalogWebTestSupport;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ProductsActivationHistoryFilterContractIntegrationTest {

    private static final String DEACTIVATION_REASON = "Deactivated for contract test";
    private static final String REACTIVATION_REASON = "Reactivated for contract test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsUnfilteredActivationHistoryWhenNoFiltersProvided() throws Exception {
        String sku = "ARC-3800";
        String tenantA = "TEN3801";
        String actorA = "actor-a@arcanaerp.com";
        String tenantB = "TEN3802";
        String actorB = "actor-b@arcanaerp.com";

        seedActivationHistory(sku, tenantA, actorA, tenantB, actorB);

        mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].tenantCode").value(tenantB))
            .andExpect(jsonPath("$.items[0].changedBy").value(actorB))
            .andExpect(jsonPath("$.items[0].reason").value(REACTIVATION_REASON))
            .andExpect(jsonPath("$.items[1].tenantCode").value(tenantA))
            .andExpect(jsonPath("$.items[1].changedBy").value(actorA))
            .andExpect(jsonPath("$.items[1].reason").value(DEACTIVATION_REASON));
    }

    @Test
    void filtersActivationHistoryByTenantCode() throws Exception {
        String sku = "ARC-3801";
        String tenantA = "TEN3811";
        String actorA = "actor-c@arcanaerp.com";
        String tenantB = "TEN3812";
        String actorB = "actor-d@arcanaerp.com";

        seedActivationHistory(sku, tenantA, actorA, tenantB, actorB);

        mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
            .param("page", "0")
            .param("size", "10")
            .param("tenantCode", tenantA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value(tenantA))
            .andExpect(jsonPath("$.items[0].changedBy").value(actorA))
            .andExpect(jsonPath("$.items[0].reason").value(DEACTIVATION_REASON));
    }

    @Test
    void filtersActivationHistoryByChangedBy() throws Exception {
        String sku = "ARC-3802";
        String tenantA = "TEN3821";
        String actorA = "actor-e@arcanaerp.com";
        String tenantB = "TEN3822";
        String actorB = "actor-f@arcanaerp.com";

        seedActivationHistory(sku, tenantA, actorA, tenantB, actorB);

        mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
            .param("page", "0")
            .param("size", "10")
            .param("changedBy", actorB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value(tenantB))
            .andExpect(jsonPath("$.items[0].changedBy").value(actorB))
            .andExpect(jsonPath("$.items[0].reason").value(REACTIVATION_REASON));
    }

    @Test
    void filtersActivationHistoryByTenantCodeAndChangedBy() throws Exception {
        String sku = "ARC-3803";
        String tenantA = "TEN3831";
        String actorA = "actor-g@arcanaerp.com";
        String tenantB = "TEN3832";
        String actorB = "actor-h@arcanaerp.com";

        seedActivationHistory(sku, tenantA, actorA, tenantB, actorB);

        mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
            .param("page", "0")
            .param("size", "10")
            .param("tenantCode", tenantB)
            .param("changedBy", actorB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value(tenantB))
            .andExpect(jsonPath("$.items[0].changedBy").value(actorB))
            .andExpect(jsonPath("$.items[0].reason").value(REACTIVATION_REASON));
    }

    @Test
    void filtersActivationHistoryByCurrentActive() throws Exception {
        String sku = "ARC-3804";
        String tenantA = "TEN3841";
        String actorA = "actor-i@arcanaerp.com";
        String tenantB = "TEN3842";
        String actorB = "actor-j@arcanaerp.com";

        seedActivationHistory(sku, tenantA, actorA, tenantB, actorB);

        mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
            .param("page", "0")
            .param("size", "10")
            .param("currentActive", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value(tenantB))
            .andExpect(jsonPath("$.items[0].changedBy").value(actorB))
            .andExpect(jsonPath("$.items[0].reason").value(REACTIVATION_REASON));

        mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
            .param("page", "0")
            .param("size", "10")
            .param("currentActive", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value(tenantA))
            .andExpect(jsonPath("$.items[0].changedBy").value(actorA))
            .andExpect(jsonPath("$.items[0].reason").value(DEACTIVATION_REASON));
    }

    @Test
    void filtersActivationHistoryByTenantCodeChangedByAndCurrentActive() throws Exception {
        String sku = "ARC-3805";
        String tenantA = "TEN3851";
        String actorA = "actor-k@arcanaerp.com";
        String tenantB = "TEN3852";
        String actorB = "actor-l@arcanaerp.com";

        seedActivationHistory(sku, tenantA, actorA, tenantB, actorB);

        mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
            .param("page", "0")
            .param("size", "10")
            .param("tenantCode", tenantB)
            .param("changedBy", actorB)
            .param("currentActive", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].tenantCode").value(tenantB))
            .andExpect(jsonPath("$.items[0].changedBy").value(actorB))
            .andExpect(jsonPath("$.items[0].reason").value(REACTIVATION_REASON));
    }

    @Test
    void rejectsBlankTenantCodeFilter() throws Exception {
        mockMvc.perform(get("/api/products/arc-3890/activation-history")
            .param("page", "0")
            .param("size", "10")
            .param("tenantCode", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("tenantCode query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/products/arc-3890/activation-history"));
    }

    @Test
    void rejectsBlankChangedByFilter() throws Exception {
        mockMvc.perform(get("/api/products/arc-3891/activation-history")
            .param("page", "0")
            .param("size", "10")
            .param("changedBy", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("changedBy query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/products/arc-3891/activation-history"));
    }

    @Test
    void rejectsBlankCurrentActiveFilter() throws Exception {
        mockMvc.perform(get("/api/products/arc-3891a/activation-history")
            .param("page", "0")
            .param("size", "10")
            .param("currentActive", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("currentActive query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/products/arc-3891a/activation-history"));
    }

    @Test
    void rejectsInvalidCurrentActiveFilter() throws Exception {
        mockMvc.perform(get("/api/products/arc-3891b/activation-history")
            .param("page", "0")
            .param("size", "10")
            .param("currentActive", "yes"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("currentActive query parameter must be either true or false"))
            .andExpect(jsonPath("$.path").value("/api/products/arc-3891b/activation-history"));
    }

    @Test
    void filtersActivationHistoryByChangedAtRange() throws Exception {
        String sku = "ARC-3892";
        String tenantA = "TEN3892A";
        String actorA = "actor-range-a@arcanaerp.com";
        String tenantB = "TEN3892B";
        String actorB = "actor-range-b@arcanaerp.com";

        seedActivationHistory(sku, tenantA, actorA, tenantB, actorB);

        MvcResult result = mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).path("items");
        String latestChangedAt = items.get(0).path("changedAt").asText();
        String olderChangedAt = items.get(1).path("changedAt").asText();

        mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
            .param("page", "0")
            .param("size", "10")
            .param("changedAtFrom", latestChangedAt)
            .param("changedAtTo", latestChangedAt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].changedAt").value(latestChangedAt));

        mockMvc.perform(get("/api/products/{sku}/activation-history", sku)
            .param("page", "0")
            .param("size", "10")
            .param("changedAtFrom", olderChangedAt)
            .param("changedAtTo", olderChangedAt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].changedAt").value(olderChangedAt));
    }

    @Test
    void rejectsInvalidChangedAtFromFormat() throws Exception {
        mockMvc.perform(get("/api/products/arc-3893/activation-history")
            .param("page", "0")
            .param("size", "10")
            .param("changedAtFrom", "not-a-timestamp"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("changedAtFrom query parameter must be a valid ISO-8601 instant"))
            .andExpect(jsonPath("$.path").value("/api/products/arc-3893/activation-history"));
    }

    @Test
    void rejectsInvalidChangedAtRangeOrder() throws Exception {
        mockMvc.perform(get("/api/products/arc-3894/activation-history")
            .param("page", "0")
            .param("size", "10")
            .param("changedAtFrom", "2026-03-02T00:00:00Z")
            .param("changedAtTo", "2026-03-01T00:00:00Z"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"))
            .andExpect(jsonPath("$.path").value("/api/products/arc-3894/activation-history"));
    }

    private void seedActivationHistory(
        String sku,
        String tenantA,
        String actorA,
        String tenantB,
        String actorB
    ) throws Exception {
        createProduct(sku);
        registerActor(tenantA, actorA);
        changeActivation(sku, false, tenantA, actorA, DEACTIVATION_REASON);
        Thread.sleep(25);
        registerActor(tenantB, actorB);
        changeActivation(sku, true, tenantB, actorB, REACTIVATION_REASON);
    }

    private void createProduct(String sku) throws Exception {
        ProductCatalogWebTestSupport.createProduct(
            mockMvc,
            sku,
            "Contract Product " + sku,
            "kits",
            "Kits",
            "9.99",
            "USD"
        )
            .andExpect(status().isCreated());
    }

    private void changeActivation(
        String sku,
        boolean active,
        String tenantCode,
        String changedBy,
        String reason
    ) throws Exception {
        ActorActivationWebTestSupport.setProductActive(mockMvc, sku, active, tenantCode, changedBy, reason)
            .andExpect(status().isOk());
    }

    private void registerActor(String tenantCode, String email) throws Exception {
        ActorActivationWebTestSupport.registerActor(mockMvc, tenantCode, email, "Contract Tenant", "Activation Actor")
            .andExpect(status().isCreated());
    }
}
