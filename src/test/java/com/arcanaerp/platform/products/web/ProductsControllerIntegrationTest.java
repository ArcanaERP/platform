package com.arcanaerp.platform.products.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
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
              "active": false
            }
            """;

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(patch("/api/products/arc-3000/active")
            .contentType(MediaType.APPLICATION_JSON)
            .content(deactivatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-3000"))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.deactivatedAt").isNotEmpty());
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
              "active": false
            }
            """;

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(activePayload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(inactivePayload))
            .andExpect(status().isCreated());

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
}
