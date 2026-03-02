package com.arcanaerp.platform.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class StatusHistoryWebTestSupport {

    private StatusHistoryWebTestSupport() {}

    public static MockHttpServletRequestBuilder statusHistoryRequest(String path) {
        return get(path);
    }

    public static MockHttpServletRequestBuilder statusHistoryRequest(
        String path,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        MockHttpServletRequestBuilder request = statusHistoryRequest(path);
        if (page != null) {
            request.param("page", String.valueOf(page));
        }
        if (size != null) {
            request.param("size", String.valueOf(size));
        }
        if (optionalNameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("optionalNameValuePairs must have an even number of elements");
        }
        for (int index = 0; index < optionalNameValuePairs.length; index += 2) {
            String name = optionalNameValuePairs[index];
            String value = optionalNameValuePairs[index + 1];
            if (value != null) {
                request.param(name, value);
            }
        }
        return request;
    }
}
