package com.arcanaerp.platform.commerce.web;

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
class CommerceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsReadsAndListsStorefronts() throws Exception {
        CommerceWebIntegrationTestSupport.createStorefront(
            mockMvc,
            "commerceweb01",
            "b2c-main",
            "B2C Main",
            "USD",
            "en-US",
            true
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("COMMERCEWEB01"))
            .andExpect(jsonPath("$.storefrontCode").value("B2C-MAIN"))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.active").value(true));

        CommerceWebIntegrationTestSupport.createStorefront(
            mockMvc,
            "commerceweb01",
            "wholesale",
            "Wholesale",
            "USD",
            "en-US",
            false
        )
            .andExpect(status().isCreated());

        mockMvc.perform(CommerceWebIntegrationTestSupport.getStorefrontRequest("commerceweb01", "b2c-main"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.storefrontCode").value("B2C-MAIN"))
            .andExpect(jsonPath("$.name").value("B2C Main"));

        mockMvc.perform(
            CommerceWebIntegrationTestSupport.listStorefrontsRequest(
                "commerceweb01",
                0,
                10,
                "active", "true"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.storefrontCode=='B2C-MAIN')].name", hasItem("B2C Main")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        CommerceWebIntegrationTestSupport.createStorefront(
            mockMvc,
            "commerceweb02",
            "b2c-main",
            "B2C Main",
            "USD",
            "en-US",
            true
        )
            .andExpect(status().isCreated());

        mockMvc.perform(CommerceWebIntegrationTestSupport.listStorefrontsRequest("commerceweb02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.storefrontCode=='B2C-MAIN')].name", hasItem("B2C Main")));
    }

    @Test
    void rejectsDuplicateTenantLocalStorefrontCodes() throws Exception {
        CommerceWebIntegrationTestSupport.createStorefront(
            mockMvc,
            "commerceweb03",
            "b2c-main",
            "B2C Main",
            "USD",
            "en-US",
            true
        )
            .andExpect(status().isCreated());

        CommerceWebIntegrationTestSupport.createStorefront(
            mockMvc,
            "commerceweb03",
            "B2C-MAIN",
            "Duplicate",
            "USD",
            "en-US",
            true
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Storefront already exists for tenant/code: COMMERCEWEB03/B2C-MAIN"))
            .andExpect(jsonPath("$.path").value("/api/commerce/storefronts"));
    }

    @Test
    void returnsNotFoundForMissingStorefront() throws Exception {
        mockMvc.perform(CommerceWebIntegrationTestSupport.getStorefrontRequest("commerceweb04", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Storefront not found for tenant/code: COMMERCEWEB04/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/commerce/storefronts/missing"));
    }

    @Test
    void rejectsInvalidPaginationAndValidation() throws Exception {
        CommerceWebIntegrationTestSupport.createStorefront(
            mockMvc,
            "commerceweb05",
            "bad-currency",
            "Bad Currency",
            "US",
            "en-US",
            true
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("currencyCode must be a 3-letter ISO code"));

        mockMvc.perform(CommerceWebIntegrationTestSupport.listStorefrontsRequest("commerceweb05", -1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"));
    }
}
