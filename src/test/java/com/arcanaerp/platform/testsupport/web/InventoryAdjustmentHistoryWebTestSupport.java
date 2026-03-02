package com.arcanaerp.platform.testsupport.web;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class InventoryAdjustmentHistoryWebTestSupport {

    private InventoryAdjustmentHistoryWebTestSupport() {}

    public static MockHttpServletRequestBuilder adjustmentsRequest(String sku) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(adjustmentsPath(sku));
    }

    public static MockHttpServletRequestBuilder adjustmentsRequest(String sku, Integer page, Integer size) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(adjustmentsPath(sku), page, size);
    }

    public static MockHttpServletRequestBuilder adjustmentsRequest(
        String sku,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(
            adjustmentsPath(sku),
            page,
            size,
            optionalNameValuePairs
        );
    }

    private static String adjustmentsPath(String sku) {
        return "/api/inventory/" + sku + "/adjustments";
    }
}
