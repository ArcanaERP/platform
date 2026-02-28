package com.arcanaerp.platform.products.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductsControllerIntegrationTest {

    private static final String LEGACY_TENANT_CODE = "TEN3000";
    private static final String FILTER_TENANT_CODE = "TEN3200";
    private static final String HISTORY_TENANT_CODE = "TEN3300";
    private static final String UNKNOWN_TENANT_CODE = "TEN3400";
    private static final String MISMATCH_ACTOR_TENANT_CODE = "TEN3501";
    private static final String MISMATCH_REQUEST_TENANT_CODE = "TEN3502";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsProduct() throws Exception {
        String payload = """
            {
              "sku": "ARC-1000",
              "name": "Starter Kit",
              "categoryCode": "kits",
              "categoryName": "Kits",
              "amount": 19.99,
              "currencyCode": "usd"
            }
            """;

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("ARC-1000"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.activatedAt").isNotEmpty())
            .andExpect(jsonPath("$.deactivatedAt").value(nullValue()))
            .andExpect(jsonPath("$.lastActivationChangeReason").value(nullValue()))
            .andExpect(jsonPath("$.lastActivationChangedAt").value(nullValue()))
            .andExpect(jsonPath("$.categoryCode").value("KITS"))
            .andExpect(jsonPath("$.currentPrice").value(19.99))
            .andExpect(jsonPath("$.currencyCode").value("USD"));

        mockMvc.perform(get("/api/products?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.sku=='ARC-1000')].categoryName", hasItem("Kits")));
    }

    @Test
    void canDeactivateProduct() throws Exception {
        String createPayload = """
            {
              "sku": "ARC-3000",
              "name": "Legacy Kit",
              "categoryCode": "kits",
              "categoryName": "Kits",
              "amount": 9.99,
              "currencyCode": "USD"
            }
            """;
        String deactivatePayload = """
            {
              "active": false,
              "reason": "Discontinued by product team",
              "tenantCode": "%s",
              "changedBy": "product-team@arcanaerp.com"
            }
            """.formatted(LEGACY_TENANT_CODE);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true));

        registerActor(LEGACY_TENANT_CODE, "product-team@arcanaerp.com");
        mockMvc.perform(patch("/api/products/arc-3000/active")
            .contentType(MediaType.APPLICATION_JSON)
            .content(deactivatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-3000"))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.deactivatedAt").isNotEmpty())
            .andExpect(jsonPath("$.lastActivationChangeReason").value("Discontinued by product team"))
            .andExpect(jsonPath("$.lastActivationChangedAt").isNotEmpty());
    }

    @Test
    void canFilterProductsByActiveFlag() throws Exception {
        String activePayload = """
            {
              "sku": "ARC-3100",
              "name": "Active Kit",
              "categoryCode": "kits",
              "categoryName": "Kits",
              "amount": 9.99,
              "currencyCode": "USD"
            }
            """;
        String inactivePayload = """
            {
              "sku": "ARC-3200",
              "name": "Inactive Kit",
              "categoryCode": "kits",
              "categoryName": "Kits",
              "amount": 9.99,
              "currencyCode": "USD"
            }
            """;
        String deactivatePayload = """
            {
              "active": false,
              "reason": "Retired in filter test",
              "tenantCode": "%s",
              "changedBy": "catalog-admin@arcanaerp.com"
            }
            """.formatted(FILTER_TENANT_CODE);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(activePayload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(inactivePayload))
            .andExpect(status().isCreated());

        registerActor(FILTER_TENANT_CODE, "catalog-admin@arcanaerp.com");
        mockMvc.perform(patch("/api/products/arc-3200/active")
            .contentType(MediaType.APPLICATION_JSON)
            .content(deactivatePayload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/products?page=0&size=20&active=true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.sku=='ARC-3100')].active", hasItem(true)));

        mockMvc.perform(get("/api/products?page=0&size=20&active=false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.sku=='ARC-3200')].active", hasItem(false)));
    }

    @Test
    void canListProductActivationHistory() throws Exception {
        String createPayload = """
            {
              "sku": "ARC-3300",
              "name": "History Kit",
              "categoryCode": "kits",
              "categoryName": "Kits",
              "amount": 9.99,
              "currencyCode": "USD"
            }
            """;
        String deactivatePayload = """
            {
              "active": false,
              "reason": "Initial retirement",
              "tenantCode": "%s",
              "changedBy": "ops@arcanaerp.com"
            }
            """.formatted(HISTORY_TENANT_CODE);
        String reactivatePayload = """
            {
              "active": true,
              "reason": "Customer demand rebound",
              "tenantCode": "%s",
              "changedBy": "sales@arcanaerp.com"
            }
            """.formatted(HISTORY_TENANT_CODE);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(createPayload))
            .andExpect(status().isCreated());

        registerActor(HISTORY_TENANT_CODE, "ops@arcanaerp.com");
        mockMvc.perform(patch("/api/products/arc-3300/active")
            .contentType(MediaType.APPLICATION_JSON)
            .content(deactivatePayload))
            .andExpect(status().isOk());

        registerActor(HISTORY_TENANT_CODE, "sales@arcanaerp.com");
        mockMvc.perform(patch("/api/products/arc-3300/active")
            .contentType(MediaType.APPLICATION_JSON)
            .content(reactivatePayload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/arc-3300/activation-history?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-3300"))
            .andExpect(jsonPath("$.items[0].tenantCode").value(HISTORY_TENANT_CODE))
            .andExpect(jsonPath("$.items[0].previousActive").value(false))
            .andExpect(jsonPath("$.items[0].currentActive").value(true))
            .andExpect(jsonPath("$.items[0].reason").value("Customer demand rebound"))
            .andExpect(jsonPath("$.items[0].changedBy").value("sales@arcanaerp.com"))
            .andExpect(jsonPath("$.items[1].tenantCode").value(HISTORY_TENANT_CODE))
            .andExpect(jsonPath("$.items[1].changedBy").value("ops@arcanaerp.com"))
            .andExpect(jsonPath("$.items[1].reason").value("Initial retirement"));
    }

    @Test
    void activationHistoryReturnsNotFoundForUnknownSku() throws Exception {
        mockMvc.perform(get("/api/products/arc-missing/activation-history?page=0&size=10"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message", startsWith("Product not found: ARC-MISSING")))
            .andExpect(jsonPath("$.path").value("/api/products/arc-missing/activation-history"));
    }

    @Test
    void rejectsActivationChangeWhenActorUnknown() throws Exception {
        String createPayload = """
            {
              "sku": "ARC-3400",
              "name": "Actor Check Kit",
              "categoryCode": "kits",
              "categoryName": "Kits",
              "amount": 9.99,
              "currencyCode": "USD"
            }
            """;
        String deactivatePayload = """
            {
              "active": false,
              "reason": "Unverified actor",
              "tenantCode": "%s",
              "changedBy": "unknown@arcanaerp.com"
            }
            """.formatted(UNKNOWN_TENANT_CODE);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(createPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/products/arc-3400/active")
            .contentType(MediaType.APPLICATION_JSON)
            .content(deactivatePayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                .value("Activation actor not found in tenant " + UNKNOWN_TENANT_CODE + ": unknown@arcanaerp.com"))
            .andExpect(jsonPath("$.path").value("/api/products/arc-3400/active"));
    }

    @Test
    void rejectsActivationChangeWhenActorExistsInDifferentTenant() throws Exception {
        String createPayload = """
            {
              "sku": "ARC-3500",
              "name": "Tenant Scoped Actor Kit",
              "categoryCode": "kits",
              "categoryName": "Kits",
              "amount": 9.99,
              "currencyCode": "USD"
            }
            """;
        String deactivatePayload = """
            {
              "active": false,
              "reason": "Tenant mismatch actor",
              "tenantCode": "%s",
              "changedBy": "tenant.actor@arcanaerp.com"
            }
            """.formatted(MISMATCH_REQUEST_TENANT_CODE);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(createPayload))
            .andExpect(status().isCreated());

        registerActor(MISMATCH_ACTOR_TENANT_CODE, "tenant.actor@arcanaerp.com");

        mockMvc.perform(patch("/api/products/arc-3500/active")
            .contentType(MediaType.APPLICATION_JSON)
            .content(deactivatePayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                .value("Activation actor not found in tenant "
                    + MISMATCH_REQUEST_TENANT_CODE
                    + ": tenant.actor@arcanaerp.com"))
            .andExpect(jsonPath("$.path").value("/api/products/arc-3500/active"));
    }

    @Test
    void returnsErrorEnvelopeForDuplicateSku() throws Exception {
        String payload = """
            {
              "sku": "ARC-2000",
              "name": "Toolkit",
              "categoryCode": "kits",
              "categoryName": "Kits",
              "amount": 29.99,
              "currencyCode": "USD"
            }
            """;

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/products"));
    }

    @Test
    void returnsErrorEnvelopeForInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/products?page=0&size=0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"))
            .andExpect(jsonPath("$.path").value("/api/products"));
    }

    private void registerActor(String tenantCode, String email) throws Exception {
        String payload = """
            {
              "tenantCode": "%s",
              "tenantName": "Activation Tenant %s",
              "roleCode": "OPS",
              "roleName": "Operations",
              "email": "%s",
              "displayName": "Activation Actor"
            }
            """.formatted(tenantCode, tenantCode, email);

        mockMvc.perform(post("/api/identity/users").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
    }
}
