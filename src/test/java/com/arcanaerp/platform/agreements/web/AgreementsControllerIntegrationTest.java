package com.arcanaerp.platform.agreements.web;

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
class AgreementsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAgreement() throws Exception {
        String payload = """
            {
              "agreementNumber": " agr-3000 ",
              "name": "  Master Services Agreement ",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.agreementNumber").value("AGR-3000"))
            .andExpect(jsonPath("$.name").value("Master Services Agreement"))
            .andExpect(jsonPath("$.agreementType").value("SERVICE"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.effectiveFrom").value("2026-03-01T00:00:00Z"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void returnsErrorEnvelopeForDuplicateAgreementNumber() throws Exception {
        String payload = """
            {
              "agreementNumber": "agr-3001",
              "name": "Master Services Agreement",
              "agreementType": "service",
              "effectiveFrom": "2026-03-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Agreement number already exists: AGR-3001"))
            .andExpect(jsonPath("$.path").value("/api/agreements"));
    }
}
