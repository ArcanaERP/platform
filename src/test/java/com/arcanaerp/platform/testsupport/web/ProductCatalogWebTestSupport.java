package com.arcanaerp.platform.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class ProductCatalogWebTestSupport {

    private ProductCatalogWebTestSupport() {}

    public static ResultActions createProduct(
        MockMvc mockMvc,
        String sku,
        String name,
        String categoryCode,
        String categoryName,
        String amount,
        String currencyCode
    ) throws Exception {
        String payload = """
            {
              "sku": "%s",
              "name": "%s",
              "categoryCode": "%s",
              "categoryName": "%s",
              "amount": %s,
              "currencyCode": "%s"
            }
            """.formatted(sku, name, categoryCode, categoryName, amount, currencyCode);

        return mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
    }

    public static ResultActions createProductWithDerivedCategory(
        MockMvc mockMvc,
        String sku,
        String namePrefix,
        String categoryName,
        String amount,
        String currencyCode
    ) throws Exception {
        String normalizedSku = normalizeSku(sku);
        String categoryCode = categoryCodeFromSku(normalizedSku);
        return createProduct(
            mockMvc,
            sku,
            namePrefix + " " + normalizedSku,
            categoryCode,
            categoryName,
            amount,
            currencyCode
        );
    }

    private static String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }

    private static String categoryCodeFromSku(String normalizedSku) {
        String sanitized = normalizedSku.replaceAll("[^A-Z0-9]", "");
        String fullCode = "CAT" + sanitized;
        return fullCode.substring(0, Math.min(32, fullCode.length()));
    }
}
