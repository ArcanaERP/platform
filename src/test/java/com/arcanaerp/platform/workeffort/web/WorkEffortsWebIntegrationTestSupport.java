package com.arcanaerp.platform.workeffort.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class WorkEffortsWebIntegrationTestSupport {

    private static final String WORK_EFFORTS_PATH = "/api/work-efforts";

    private WorkEffortsWebIntegrationTestSupport() {}

    static ResultActions createWorkEffort(
        MockMvc mockMvc,
        String tenantCode,
        String effortNumber,
        String name,
        String description,
        String status,
        String assignedTo,
        String dueAt
    ) throws Exception {
        String formattedDueAt = dueAt == null ? "null" : "\"" + dueAt + "\"";
        return mockMvc.perform(post(WORK_EFFORTS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "effortNumber": "%s",
                  "name": "%s",
                  "description": "%s",
                  "status": "%s",
                  "assignedTo": "%s",
                  "dueAt": %s
                }
                """.formatted(
                tenantCode,
                effortNumber,
                name,
                description,
                status,
                assignedTo,
                formattedDueAt
            )));
    }

    static MockHttpServletRequestBuilder getWorkEffortRequest(String tenantCode, String effortNumber) {
        return get(WORK_EFFORTS_PATH + "/" + effortNumber).param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder getWorkEffortAssignmentRequest(String tenantCode, String effortNumber) {
        return get(WORK_EFFORTS_PATH + "/" + effortNumber + "/assignment").param("tenantCode", tenantCode);
    }

    static ResultActions changeWorkEffortStatus(
        MockMvc mockMvc,
        String tenantCode,
        String effortNumber,
        String status,
        String reason,
        String changedBy
    ) throws Exception {
        return mockMvc.perform(patch(WORK_EFFORTS_PATH + "/" + effortNumber + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "status": "%s",
                  "reason": "%s",
                  "changedBy": "%s"
                }
                """.formatted(tenantCode, status, reason, changedBy)));
    }

    static ResultActions assignWorkEffort(
        MockMvc mockMvc,
        String tenantCode,
        String effortNumber,
        String assignedTo,
        String reason,
        String assignedBy
    ) throws Exception {
        return mockMvc.perform(patch(WORK_EFFORTS_PATH + "/" + effortNumber + "/assignment")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "assignedTo": "%s",
                  "reason": "%s",
                  "assignedBy": "%s"
                }
                """.formatted(tenantCode, assignedTo, reason, assignedBy)));
    }

    static MockHttpServletRequestBuilder listWorkEffortsRequest(String tenantCode, int page, int size, String... optionalNameValuePairs) {
        MockHttpServletRequestBuilder builder = get(WORK_EFFORTS_PATH)
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

    static MockHttpServletRequestBuilder listWorkEffortsRequest(String tenantCode) {
        return get(WORK_EFFORTS_PATH).param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder workEffortAssignmentActivitySummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = get(WORK_EFFORTS_PATH + "/assignment-activity-summary")
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

    static MockHttpServletRequestBuilder dailyWorkEffortAssignmentActivitySummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return assignmentActivityBucketSummaryRequest("daily", tenantCode, page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder weeklyWorkEffortAssignmentActivitySummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return assignmentActivityBucketSummaryRequest("weekly", tenantCode, page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder monthlyWorkEffortAssignmentActivitySummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return assignmentActivityBucketSummaryRequest("monthly", tenantCode, page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder dailyWorkEffortStatusActivitySummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityBucketSummaryRequest("daily", tenantCode, page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder weeklyWorkEffortStatusActivitySummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityBucketSummaryRequest("weekly", tenantCode, page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder monthlyWorkEffortStatusActivitySummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityBucketSummaryRequest("monthly", tenantCode, page, size, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder dailyWorkEffortStatusActivityByCurrentStatusSummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityBucketSummaryRequest("daily", tenantCode, page, size, true, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder weeklyWorkEffortStatusActivityByCurrentStatusSummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityBucketSummaryRequest("weekly", tenantCode, page, size, true, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder monthlyWorkEffortStatusActivityByCurrentStatusSummaryRequest(
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityBucketSummaryRequest("monthly", tenantCode, page, size, true, optionalNameValuePairs);
    }

    private static MockHttpServletRequestBuilder assignmentActivityBucketSummaryRequest(
        String bucket,
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = get(WORK_EFFORTS_PATH + "/assignment-activity/" + bucket + "-summary")
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

    private static MockHttpServletRequestBuilder statusActivityBucketSummaryRequest(
        String bucket,
        String tenantCode,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        return statusActivityBucketSummaryRequest(bucket, tenantCode, page, size, false, optionalNameValuePairs);
    }

    private static MockHttpServletRequestBuilder statusActivityBucketSummaryRequest(
        String bucket,
        String tenantCode,
        int page,
        int size,
        boolean byCurrentStatus,
        String... optionalNameValuePairs
    ) {
        String path = WORK_EFFORTS_PATH + "/status-activity/" + bucket + "-summary";
        if (byCurrentStatus) {
            path = path + "/by-current-status";
        }
        MockHttpServletRequestBuilder builder = get(path)
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

    static MockHttpServletRequestBuilder workEffortStatusHistoryRequest(
        String tenantCode,
        String effortNumber,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = get(WORK_EFFORTS_PATH + "/" + effortNumber + "/status-history")
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

    static MockHttpServletRequestBuilder workEffortAssignmentHistoryRequest(
        String tenantCode,
        String effortNumber,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = get(WORK_EFFORTS_PATH + "/" + effortNumber + "/assignment-history")
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
}
