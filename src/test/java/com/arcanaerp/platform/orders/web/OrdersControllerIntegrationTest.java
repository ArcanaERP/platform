package com.arcanaerp.platform.orders.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
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
class OrdersControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsOrder() throws Exception {
        String payload = """
            {
              "orderNumber": "so-5000",
              "customerEmail": "BUYER@ACME.COM",
              "currencyCode": "usd",
              "lines": [
                { "productSku": "arc-1000", "quantity": 2, "unitPrice": 10.00 },
                { "productSku": "arc-2000", "quantity": 1, "unitPrice": 5.50 }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderNumber").value("SO-5000"))
            .andExpect(jsonPath("$.customerEmail").value("buyer@acme.com"))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.totalAmount").value(25.5))
            .andExpect(jsonPath("$.lines[0].lineNo").value(1));

        mockMvc.perform(get("/api/orders?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.orderNumber=='SO-5000')].customerEmail", hasItem("buyer@acme.com")));
    }

    @Test
    void returnsErrorEnvelopeForDuplicateOrderNumber() throws Exception {
        String payload = """
            {
              "orderNumber": "so-5001",
              "customerEmail": "buyer@acme.com",
              "currencyCode": "USD",
              "lines": [
                { "productSku": "arc-1000", "quantity": 1, "unitPrice": 10.00 }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/orders"));
    }
}
