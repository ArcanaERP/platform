package com.arcanaerp.platform.commerce.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.arcanaerp.platform.testsupport.web.ProductCatalogWebTestSupport;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class CommerceWebIntegrationTestSupport {

    private static final String STOREFRONTS_PATH = "/api/commerce/storefronts";

    private CommerceWebIntegrationTestSupport() {}

    static ResultActions createStorefront(
        MockMvc mockMvc,
        String tenantCode,
        String storefrontCode,
        String name,
        String currencyCode,
        String defaultLanguageTag,
        boolean active
    ) throws Exception {
        return mockMvc.perform(post(STOREFRONTS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "storefrontCode": "%s",
                  "name": "%s",
                  "currencyCode": "%s",
                  "defaultLanguageTag": "%s",
                  "active": %s
                }
                """.formatted(
                tenantCode,
                storefrontCode,
                name,
                currencyCode,
                defaultLanguageTag,
                active
            )));
    }

    static MockHttpServletRequestBuilder getStorefrontRequest(String tenantCode, String storefrontCode) {
        return get(STOREFRONTS_PATH + "/" + storefrontCode).param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder listStorefrontsRequest(String tenantCode, int page, int size, String... optionalNameValuePairs) {
        MockHttpServletRequestBuilder builder = get(STOREFRONTS_PATH)
            .param("tenantCode", tenantCode)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
        if (optionalNameValuePairs == null || optionalNameValuePairs.length == 0) {
            return builder;
        }
        if (optionalNameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("optionalNameValuePairs must contain name/value pairs");
        }
        for (int i = 0; i < optionalNameValuePairs.length; i += 2) {
            builder.param(optionalNameValuePairs[i], optionalNameValuePairs[i + 1]);
        }
        return builder;
    }

    static MockHttpServletRequestBuilder listStorefrontsRequest(String tenantCode) {
        return get(STOREFRONTS_PATH).param("tenantCode", tenantCode);
    }

    static ResultActions assignStorefrontProduct(
        MockMvc mockMvc,
        String tenantCode,
        String storefrontCode,
        String sku,
        String merchandisingName,
        int position,
        boolean active
    ) throws Exception {
        String formattedMerchandisingName = merchandisingName == null ? "null" : "\"%s\"".formatted(merchandisingName);
        return mockMvc.perform(post(STOREFRONTS_PATH + "/" + storefrontCode + "/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "sku": "%s",
                  "merchandisingName": %s,
                  "position": %s,
                  "active": %s
                }
                """.formatted(tenantCode, sku, formattedMerchandisingName, position, active)));
    }

    static MockHttpServletRequestBuilder listStorefrontProductsRequest(
        String tenantCode,
        String storefrontCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = get(STOREFRONTS_PATH + "/" + storefrontCode + "/products")
            .param("tenantCode", tenantCode)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
        if (optionalNameValuePairs == null || optionalNameValuePairs.length == 0) {
            return builder;
        }
        if (optionalNameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("optionalNameValuePairs must contain name/value pairs");
        }
        for (int i = 0; i < optionalNameValuePairs.length; i += 2) {
            builder.param(optionalNameValuePairs[i], optionalNameValuePairs[i + 1]);
        }
        return builder;
    }

    static ResultActions registerProduct(
        MockMvc mockMvc,
        String sku,
        String namePrefix,
        String categoryName
    ) throws Exception {
        return ProductCatalogWebTestSupport.createProductWithDerivedCategory(
            mockMvc,
            sku,
            namePrefix,
            categoryName,
            "10.00",
            "USD"
        );
    }
}
