package com.arcanaerp.platform.devsupport.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.devsupport.NoticeSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DevSupportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsReadsAndListsSystemNotices() throws Exception {
        DevSupportWebIntegrationTestSupport.createSystemNotice(
            mockMvc,
            "devweb01",
            "notice-001",
            "Planned Maintenance",
            "System will be unavailable overnight.",
            NoticeSeverity.WARNING,
            true
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("DEVWEB01"))
            .andExpect(jsonPath("$.noticeCode").value("NOTICE-001"))
            .andExpect(jsonPath("$.severity").value("WARNING"))
            .andExpect(jsonPath("$.active").value(true));

        DevSupportWebIntegrationTestSupport.createSystemNotice(
            mockMvc,
            "devweb01",
            "notice-002",
            "Incident Resolved",
            "Prior outage has been resolved.",
            NoticeSeverity.INFO,
            false
        )
            .andExpect(status().isCreated());

        mockMvc.perform(DevSupportWebIntegrationTestSupport.getSystemNoticeRequest("devweb01", "notice-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.noticeCode").value("NOTICE-001"))
            .andExpect(jsonPath("$.title").value("Planned Maintenance"));

        mockMvc.perform(
            DevSupportWebIntegrationTestSupport.listSystemNoticesRequest(
                "devweb01",
                0,
                10,
                "severity", "WARNING",
                "active", "true"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.noticeCode=='NOTICE-001')].title", hasItem("Planned Maintenance")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        DevSupportWebIntegrationTestSupport.createSystemNotice(
            mockMvc,
            "devweb02",
            "notice-001",
            "Security Review",
            "Quarterly review is in progress.",
            NoticeSeverity.INFO,
            true
        )
            .andExpect(status().isCreated());

        mockMvc.perform(DevSupportWebIntegrationTestSupport.listSystemNoticesRequest("devweb02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.noticeCode=='NOTICE-001')].title", hasItem("Security Review")));
    }

    @Test
    void rejectsDuplicateTenantLocalNoticeCodes() throws Exception {
        DevSupportWebIntegrationTestSupport.createSystemNotice(
            mockMvc,
            "devweb03",
            "notice-001",
            "Planned Maintenance",
            "System will be unavailable overnight.",
            NoticeSeverity.WARNING,
            true
        )
            .andExpect(status().isCreated());

        DevSupportWebIntegrationTestSupport.createSystemNotice(
            mockMvc,
            "devweb03",
            "NOTICE-001",
            "Duplicate Notice",
            "Duplicate message.",
            NoticeSeverity.ERROR,
            true
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("System notice already exists for tenant/code: DEVWEB03/NOTICE-001"))
            .andExpect(jsonPath("$.path").value("/api/dev-support/system-notices"));
    }

    @Test
    void returnsNotFoundForMissingSystemNotice() throws Exception {
        mockMvc.perform(DevSupportWebIntegrationTestSupport.getSystemNoticeRequest("devweb04", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("System notice not found for tenant/code: DEVWEB04/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/dev-support/system-notices/missing"));
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mockMvc.perform(DevSupportWebIntegrationTestSupport.listSystemNoticesRequest("devweb05", -1, 10))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to zero"));
    }
}
