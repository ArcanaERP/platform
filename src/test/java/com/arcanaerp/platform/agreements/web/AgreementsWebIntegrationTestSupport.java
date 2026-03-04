package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.testsupport.web.AgreementStatusHistoryWebTestSupport;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class AgreementsWebIntegrationTestSupport {

    private AgreementsWebIntegrationTestSupport() {}

    static MockHttpServletRequestBuilder statusHistoryRequest(String agreementNumber) {
        return AgreementStatusHistoryWebTestSupport.statusHistoryRequest(agreementNumber);
    }

    static MockHttpServletRequestBuilder statusHistoryRequest(
        String agreementNumber,
        Integer page,
        Integer size
    ) {
        return AgreementStatusHistoryWebTestSupport.statusHistoryRequest(agreementNumber, page, size);
    }

    static MockHttpServletRequestBuilder statusHistoryRequestDefault(String agreementNumber) {
        return AgreementStatusHistoryWebTestSupport.statusHistoryRequestDefault(agreementNumber);
    }

    static MockHttpServletRequestBuilder statusHistoryRequestDefault(
        String agreementNumber,
        String... optionalNameValuePairs
    ) {
        return AgreementStatusHistoryWebTestSupport.statusHistoryRequestDefault(agreementNumber, optionalNameValuePairs);
    }
}
