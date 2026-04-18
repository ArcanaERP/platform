package com.arcanaerp.platform.communicationevents.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class CommunicationEventsWebIntegrationTestSupport {

    private static final String COMMUNICATION_EVENTS_PATH = "/api/communication-events";

    private CommunicationEventsWebIntegrationTestSupport() {}

    static ResultActions createEvent(
        MockMvc mockMvc,
        String tenantCode,
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
            .content(createPayload(tenantCode, channel, direction, subject, summary, occurredAt, recordedBy, externalReference)));
    }

    static MockHttpServletRequestBuilder getEventRequest(String tenantCode, String eventNumber) {
        return get(COMMUNICATION_EVENTS_PATH + "/" + eventNumber)
            .param("tenantCode", tenantCode);
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
              "channel": "%s",
              "direction": "%s",
              "subject": "%s",
              "summary": "%s",
              "occurredAt": "%s",
              "recordedBy": "%s",
              "externalReference": %s
            }
            """.formatted(tenantCode, channel, direction, subject, summary, occurredAt, recordedBy, formattedExternalReference);
    }
}
