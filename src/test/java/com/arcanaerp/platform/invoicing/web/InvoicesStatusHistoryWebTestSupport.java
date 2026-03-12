package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.testsupport.web.StatusHistoryWebTestSupport;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class InvoicesStatusHistoryWebTestSupport {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private InvoicesStatusHistoryWebTestSupport() {}

    static MockHttpServletRequestBuilder statusHistoryRequestDefault(String invoiceNumber) {
        return statusHistoryRequest(invoiceNumber, DEFAULT_PAGE, DEFAULT_SIZE);
    }

    static MockHttpServletRequestBuilder statusHistoryRequestDefault(
        String invoiceNumber,
        String... optionalNameValuePairs
    ) {
        return statusHistoryRequest(invoiceNumber, DEFAULT_PAGE, DEFAULT_SIZE, optionalNameValuePairs);
    }

    static MockHttpServletRequestBuilder statusHistoryRequest(
        String invoiceNumber,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(
            "/api/invoices/" + invoiceNumber + "/status-history",
            page,
            size,
            optionalNameValuePairs
        );
    }

    private static MockHttpServletRequestBuilder statusHistoryRequest(String invoiceNumber, Integer page, Integer size) {
        return statusHistoryRequest(invoiceNumber, page, size, new String[0]);
    }
}
