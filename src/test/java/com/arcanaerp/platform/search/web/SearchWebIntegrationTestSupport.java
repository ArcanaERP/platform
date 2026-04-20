package com.arcanaerp.platform.search.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class SearchWebIntegrationTestSupport {

    private static final String SEARCH_ENTRIES_PATH = "/api/search/entries";

    private SearchWebIntegrationTestSupport() {}

    static ResultActions createSearchEntry(
        MockMvc mockMvc,
        String tenantCode,
        String entryNumber,
        String title,
        String snippet,
        String category,
        String targetType,
        String targetIdentifier,
        String targetUri
    ) throws Exception {
        return mockMvc.perform(post(SEARCH_ENTRIES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantCode": "%s",
                  "entryNumber": "%s",
                  "title": "%s",
                  "snippet": "%s",
                  "category": "%s",
                  "targetType": "%s",
                  "targetIdentifier": "%s",
                  "targetUri": "%s"
                }
                """.formatted(
                tenantCode,
                entryNumber,
                title,
                snippet,
                category,
                targetType,
                targetIdentifier,
                targetUri
            )));
    }

    static MockHttpServletRequestBuilder getSearchEntryRequest(String tenantCode, String entryNumber) {
        return get(SEARCH_ENTRIES_PATH + "/" + entryNumber).param("tenantCode", tenantCode);
    }

    static MockHttpServletRequestBuilder listSearchEntriesRequest(String tenantCode, int page, int size, String... optionalNameValuePairs) {
        MockHttpServletRequestBuilder builder = get(SEARCH_ENTRIES_PATH)
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

    static MockHttpServletRequestBuilder listSearchEntriesRequest(String tenantCode) {
        return get(SEARCH_ENTRIES_PATH).param("tenantCode", tenantCode);
    }
}
