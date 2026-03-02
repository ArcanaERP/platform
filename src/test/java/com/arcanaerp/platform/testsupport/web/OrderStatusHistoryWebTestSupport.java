package com.arcanaerp.platform.testsupport.web;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class OrderStatusHistoryWebTestSupport {

    private OrderStatusHistoryWebTestSupport() {}

    public static MockHttpServletRequestBuilder statusHistoryRequest(String orderNumber) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(statusHistoryPath(orderNumber));
    }

    public static MockHttpServletRequestBuilder statusHistoryRequest(String orderNumber, Integer page, Integer size) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(statusHistoryPath(orderNumber), page, size);
    }

    public static MockHttpServletRequestBuilder statusHistoryRequest(
        String orderNumber,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(
            statusHistoryPath(orderNumber),
            page,
            size,
            optionalNameValuePairs
        );
    }

    private static String statusHistoryPath(String orderNumber) {
        return "/api/orders/" + orderNumber + "/status-history";
    }
}
