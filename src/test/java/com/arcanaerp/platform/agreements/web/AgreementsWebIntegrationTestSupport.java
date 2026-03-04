package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.testsupport.web.AgreementCatalogWebTestSupport;
import com.arcanaerp.platform.testsupport.web.AgreementStatusHistoryWebTestSupport;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class AgreementsWebIntegrationTestSupport {

    private AgreementsWebIntegrationTestSupport() {}

    static ResultActions createAgreement(MockMvc mockMvc, String agreementNumber, String name) throws Exception {
        return AgreementCatalogWebTestSupport.createAgreement(mockMvc, agreementNumber, name);
    }

    static String createAgreementPayload(String agreementNumber, String name) {
        return AgreementCatalogWebTestSupport.createAgreementPayload(agreementNumber, name);
    }

    static MockHttpServletRequestBuilder getAgreementRequest(String agreementNumber) {
        return AgreementCatalogWebTestSupport.getAgreementRequest(agreementNumber);
    }

    static MockHttpServletRequestBuilder listAgreementsRequest(Integer page, Integer size) {
        return AgreementCatalogWebTestSupport.listAgreementsRequest(page, size);
    }

    static MockHttpServletRequestBuilder listAgreementsRequest(Integer page, Integer size, String status) {
        return AgreementCatalogWebTestSupport.listAgreementsRequest(page, size, status);
    }

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
