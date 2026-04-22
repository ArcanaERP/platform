package com.arcanaerp.platform.commerce.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.ActorActivationWebTestSupport;
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

    @Test
    void assignsAndListsStorefrontProducts() throws Exception {
        CommerceWebIntegrationTestSupport.createStorefront(
            mockMvc,
            "commerceweb06",
            "b2c-main",
            "B2C Main",
            "USD",
            "en-US",
            true
        )
            .andExpect(status().isCreated());
        CommerceWebIntegrationTestSupport.registerProduct(mockMvc, "arc-600", "Arc Product", "Arc Category")
            .andExpect(status().isCreated());
        CommerceWebIntegrationTestSupport.registerProduct(mockMvc, "arc-601", "Arc Product", "Arc Category")
            .andExpect(status().isCreated());

        CommerceWebIntegrationTestSupport.assignStorefrontProduct(
            mockMvc,
            "commerceweb06",
            "b2c-main",
            "arc-600",
            "Featured Product",
            1,
            true
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.storefrontCode").value("B2C-MAIN"))
            .andExpect(jsonPath("$.sku").value("ARC-600"))
            .andExpect(jsonPath("$.currentOrderability").value("ORDERABLE"));

        CommerceWebIntegrationTestSupport.assignStorefrontProduct(
            mockMvc,
            "commerceweb06",
            "b2c-main",
            "arc-601",
            null,
            2,
            false
        )
            .andExpect(status().isCreated());

        mockMvc.perform(
            CommerceWebIntegrationTestSupport.listStorefrontProductsRequest(
                "commerceweb06",
                "b2c-main",
                0,
                10,
                "active", "true"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.sku=='ARC-600')].merchandisingName", hasItem("Featured Product")));
    }

    @Test
    void rejectsUnknownStorefrontProductSku() throws Exception {
        CommerceWebIntegrationTestSupport.createStorefront(
            mockMvc,
            "commerceweb07",
            "b2c-main",
            "B2C Main",
            "USD",
            "en-US",
            true
        )
            .andExpect(status().isCreated());

        CommerceWebIntegrationTestSupport.assignStorefrontProduct(
            mockMvc,
            "commerceweb07",
            "b2c-main",
            "missing-sku",
            null,
            0,
            true
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("storefront product SKU not found: MISSING-SKU"))
            .andExpect(jsonPath("$.path").value("/api/commerce/storefronts/b2c-main/products"));
    }

    @Test
    void changesStorefrontProductActivationAndListsHistory() throws Exception {
        CommerceWebIntegrationTestSupport.createStorefront(
            mockMvc,
            "commerceweb08",
            "b2c-main",
            "B2C Main",
            "USD",
            "en-US",
            true
        )
            .andExpect(status().isCreated());
        CommerceWebIntegrationTestSupport.registerProduct(mockMvc, "arc-800", "Arc Product", "Arc Category")
            .andExpect(status().isCreated());
        CommerceWebIntegrationTestSupport.assignStorefrontProduct(
            mockMvc,
            "commerceweb08",
            "b2c-main",
            "arc-800",
            "Featured Product",
            1,
            true
        )
            .andExpect(status().isCreated());
        ActorActivationWebTestSupport.registerActorAllowingDuplicateEmail(
            mockMvc,
            "commerceweb08",
            "merchant@commerce.com",
            "Commerce Web",
            "Merchant"
        );

        CommerceWebIntegrationTestSupport.changeStorefrontProductActivation(
            mockMvc,
            "commerceweb08",
            "b2c-main",
            "arc-800",
            false,
            "Seasonal removal",
            "MERCHANT@COMMERCE.COM"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(
            CommerceWebIntegrationTestSupport.storefrontProductActivationHistoryRequest(
                "commerceweb08",
                "b2c-main",
                "arc-800",
                0,
                10,
                "currentActive", "false"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].currentActive").value(false))
            .andExpect(jsonPath("$.items[0].reason").value("Seasonal removal"))
            .andExpect(jsonPath("$.items[0].changedBy").value("merchant@commerce.com"));
    }

    @Test
    void rejectsInvalidActivationHistoryFilters() throws Exception {
        mockMvc.perform(
            CommerceWebIntegrationTestSupport.storefrontProductActivationHistoryRequest(
                "commerceweb09",
                "b2c-main",
                "arc-900",
                0,
                10,
                "changedBy", "   "
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedBy query parameter must not be blank"));

        mockMvc.perform(
            CommerceWebIntegrationTestSupport.storefrontProductActivationHistoryRequest(
                "commerceweb09",
                "b2c-main",
                "arc-900",
                0,
                10,
                "currentActive", "yes"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("currentActive query parameter must be either true or false"));

        mockMvc.perform(
            CommerceWebIntegrationTestSupport.storefrontProductActivationHistoryRequest(
                "commerceweb09",
                "b2c-main",
                "arc-900",
                0,
                10,
                "changedAtFrom", "2026-04-23T00:00:00Z",
                "changedAtTo", "2026-04-22T00:00:00Z"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("changedAtFrom must be before or equal to changedAtTo"));
    }
}
