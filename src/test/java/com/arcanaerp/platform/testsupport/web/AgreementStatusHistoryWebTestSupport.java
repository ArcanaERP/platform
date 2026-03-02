package com.arcanaerp.platform.testsupport.web;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class AgreementStatusHistoryWebTestSupport {

    private AgreementStatusHistoryWebTestSupport() {}

    public static MockHttpServletRequestBuilder statusHistoryRequest(String agreementNumber) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(statusHistoryPath(agreementNumber));
    }

    public static MockHttpServletRequestBuilder statusHistoryRequest(String agreementNumber, Integer page, Integer size) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(statusHistoryPath(agreementNumber), page, size);
    }

    public static MockHttpServletRequestBuilder statusHistoryRequest(
        String agreementNumber,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(
            statusHistoryPath(agreementNumber),
            page,
            size,
            optionalNameValuePairs
        );
    }

    private static String statusHistoryPath(String agreementNumber) {
        return "/api/agreements/" + agreementNumber + "/status-history";
    }
}
