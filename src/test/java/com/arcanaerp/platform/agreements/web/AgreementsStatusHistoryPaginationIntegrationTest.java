package com.arcanaerp.platform.agreements.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.agreements.AgreementStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AgreementsStatusHistoryPaginationIntegrationTest {

    private static final String TENANT_CODE = "TENHPG01";
    private static final String CHANGED_BY = "history.pagination@arcanaerp.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusHistoryUsesDefaultPaginationWhenPageAndSizeAreOmitted() throws Exception {
        String agreementNumber = "agr-hpg-0001";
        seedTwoEntryStatusHistory(agreementNumber);

        mockMvc.perform(get("/api/agreements/" + agreementNumber + "/status-history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].currentStatus").value("TERMINATED"))
            .andExpect(jsonPath("$.items[1].currentStatus").value("ACTIVE"));
    }

    @Test
    void paginatesStatusHistoryAtPageBoundaries() throws Exception {
        String agreementNumber = "agr-hpg-0002";
        seedTwoEntryStatusHistory(agreementNumber);

        mockMvc.perform(get("/api/agreements/" + agreementNumber + "/status-history")
            .param("page", "0")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].previousStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("TERMINATED"));

        mockMvc.perform(get("/api/agreements/" + agreementNumber + "/status-history")
            .param("page", "1")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("ACTIVE"));

        mockMvc.perform(get("/api/agreements/" + agreementNumber + "/status-history")
            .param("page", "2")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void keepsNewestFirstOrderingWhenChangedByFilterIsApplied() throws Exception {
        String agreementNumber = "agr-hpg-0003";
        seedTwoEntryStatusHistory(agreementNumber);

        mockMvc.perform(get("/api/agreements/" + agreementNumber + "/status-history")
            .param("page", "0")
            .param("size", "10")
            .param("changedBy", CHANGED_BY.toUpperCase()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].changedBy").value(CHANGED_BY))
            .andExpect(jsonPath("$.items[0].currentStatus").value("TERMINATED"))
            .andExpect(jsonPath("$.items[1].changedBy").value(CHANGED_BY))
            .andExpect(jsonPath("$.items[1].currentStatus").value("ACTIVE"));
    }

    @Test
    void rejectsStatusHistoryWhenPageIsNegative() throws Exception {
        mockMvc.perform(get("/api/agreements/agr-hpg-invalid/status-history")
            .param("page", "-1")
            .param("size", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-hpg-invalid/status-history"));
    }

    @Test
    void rejectsStatusHistoryWhenSizeOutsideBounds() throws Exception {
        mockMvc.perform(get("/api/agreements/agr-hpg-invalid/status-history")
            .param("page", "0")
            .param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-hpg-invalid/status-history"));

        mockMvc.perform(get("/api/agreements/agr-hpg-invalid/status-history")
            .param("page", "0")
            .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"))
            .andExpect(jsonPath("$.path").value("/api/agreements/agr-hpg-invalid/status-history"));
    }

    private void seedTwoEntryStatusHistory(String agreementNumber) throws Exception {
        AgreementsIntegrationTestSupport.createAgreement(mockMvc, agreementNumber, "History Pagination Agreement");
        AgreementsIntegrationTestSupport.registerActor(
            mockMvc,
            TENANT_CODE,
            CHANGED_BY,
            "Agreements Status History Pagination Tenant",
            "Agreements Status History Pagination Actor"
        );

        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            agreementNumber,
            AgreementStatus.ACTIVE,
            TENANT_CODE,
            "Initial activation",
            CHANGED_BY
        )
            .andExpect(status().isOk());
        // Ensure deterministic newest-first assertions when sorting by changedAt descending.
        Thread.sleep(5L);
        AgreementsIntegrationTestSupport.transitionAgreementStatus(
            mockMvc,
            agreementNumber,
            AgreementStatus.TERMINATED,
            TENANT_CODE,
            "Contract terminated",
            CHANGED_BY
        )
            .andExpect(status().isOk());
    }
}
