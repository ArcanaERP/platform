package com.arcanaerp.platform.communicationevents.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class CommunicationEventsWebIntegrationTestSupport {

    private static final String COMMUNICATION_EVENTS_PATH = "/api/communication-events";
    private static final String STATUS_TYPES_PATH = "/api/communication-events/status-types";
    private static final String PURPOSE_TYPES_PATH = "/api/communication-events/purpose-types";

    private CommunicationEventsWebIntegrationTestSupport() {}

    static ResultActions createEvent(
        MockMvc mockMvc,
        String tenantCode,
        String statusCode,
        String purposeCode,
        String channel,
        String direction,
        String subject,
        String summary,
        String occurredAt,
        String recordedBy,
        String externalReference
    ) throws Exception {
        return mockMvc.perform(post(COMMUNICATION_EVENTS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPayload(
                tenantCode,
                statusCode,
                purposeCode,
                channel,
                direction,
                subject,
                summary,
                occurredAt,
                recordedBy,
                externalReference
            )));
    }

    static ResultActions createStatusType(MockMvc mockMvc, String tenantCode, String code, String name) throws Exception {
        return mockMvc.perform(post(STATUS_TYPES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "code": "%s",
                  "name": "%s"
                }
                """.formatted(tenantCode, code, name)));
    }

    static ResultActions createPurposeType(MockMvc mockMvc, String tenantCode, String code, String name) throws Exception {
        return mockMvc.perform(post(PURPOSE_TYPES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "code": "%s",
                  "name": "%s"
                }
                """.formatted(tenantCode, code, name)));
    }

    static MockHttpServletRequestBuilder listStatusTypesRequest(String tenantCode, int page, int size) {
        return get(STATUS_TYPES_PATH)
            .param("tenantCode", tenantCode)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
    }

    static MockHttpServletRequestBuilder listPurposeTypesRequest(String tenantCode, int page, int size) {
        return get(PURPOSE_TYPES_PATH)
            .param("tenantCode", tenantCode)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size));
    }

    static MockHttpServletRequestBuilder getEventRequest(String tenantCode, String eventNumber) {
        return get(COMMUNICATION_EVENTS_PATH + "/" + eventNumber)
            .param("tenantCode", tenantCode);
    }

    static ResultActions changeStatus(
        MockMvc mockMvc,
        String tenantCode,
        String eventNumber,
        String statusCode,
        String reason,
        String changedBy
    ) throws Exception {
        return mockMvc.perform(patch(COMMUNICATION_EVENTS_PATH + "/" + eventNumber + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "statusCode": "%s",
                  "reason": "%s",
                  "changedBy": "%s"
                }
                """.formatted(tenantCode, statusCode, reason, changedBy)));
    }

    static MockHttpServletRequestBuilder statusHistoryRequest(String tenantCode, String eventNumber, int page, int size) {
        return statusHistoryRequest(tenantCode, eventNumber, page, size, new String[0]);
    }

    static MockHttpServletRequestBuilder statusHistoryRequest(
        String tenantCode,
        String eventNumber,
        int page,
        int size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder builder = get(COMMUNICATION_EVENTS_PATH + "/" + eventNumber + "/status-history")
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

    static MockHttpServletRequestBuilder listEventsRequest(String tenantCode, int page, int size, String... optionalNameValuePairs) {
        MockHttpServletRequestBuilder builder = get(COMMUNICATION_EVENTS_PATH)
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

    static MockHttpServletRequestBuilder listEventsRequest(String tenantCode) {
        return get(COMMUNICATION_EVENTS_PATH).param("tenantCode", tenantCode);
    }

    static String extractJsonString(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) {
            throw new IllegalArgumentException("field not found: " + fieldName);
        }
        int valueStart = start + pattern.length();
        int valueEnd = json.indexOf('"', valueStart);
        if (valueEnd < 0) {
            throw new IllegalArgumentException("unterminated field: " + fieldName);
        }
        return json.substring(valueStart, valueEnd);
    }

    private static String createPayload(
        String tenantCode,
        String statusCode,
        String purposeCode,
        String channel,
        String direction,
        String subject,
        String summary,
        String occurredAt,
        String recordedBy,
        String externalReference
    ) {
        String formattedExternalReference = externalReference == null
            ? "null"
            : "\"" + externalReference + "\"";
        return """
            {
              "tenantCode": "%s",
              "statusCode": "%s",
              "purposeCode": "%s",
              "channel": "%s",
              "direction": "%s",
              "subject": "%s",
              "summary": "%s",
              "occurredAt": "%s",
              "recordedBy": "%s",
              "externalReference": %s
            }
            """.formatted(
            tenantCode,
            statusCode,
            purposeCode,
            channel,
            direction,
            subject,
            summary,
            occurredAt,
            recordedBy,
            formattedExternalReference
        );
    }
}
