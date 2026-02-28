package com.arcanaerp.platform.products.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
            .andExpect(jsonPath("$.categoryCode").value("KITS"))
            .andExpect(jsonPath("$.currentPrice").value(19.99))
            .andExpect(jsonPath("$.currencyCode").value("USD"));

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sku").value("ARC-1000"))
            .andExpect(jsonPath("$[0].categoryName").value("Kits"));
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
}
