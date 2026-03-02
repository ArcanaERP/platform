package com.arcanaerp.platform.testsupport.web;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class InventoryTransferHistoryWebTestSupport {

    private InventoryTransferHistoryWebTestSupport() {}

    public static MockHttpServletRequestBuilder transfersRequest(String sku) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(transfersPath(sku));
    }

    public static MockHttpServletRequestBuilder transfersRequest(String sku, Integer page, Integer size) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(transfersPath(sku), page, size);
    }

    public static MockHttpServletRequestBuilder transfersRequest(
        String sku,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(
            transfersPath(sku),
            page,
            size,
            optionalNameValuePairs
        );
    }

    private static String transfersPath(String sku) {
        return "/api/inventory/" + sku + "/transfers";
    }
}
