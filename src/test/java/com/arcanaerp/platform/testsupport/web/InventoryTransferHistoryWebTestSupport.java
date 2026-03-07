package com.arcanaerp.platform.testsupport.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class InventoryTransferHistoryWebTestSupport {

    private static final int DEFAULT_PAGE = PageQuery.DEFAULT_PAGE;
    private static final int DEFAULT_SIZE = PageQuery.DEFAULT_SIZE;

    private InventoryTransferHistoryWebTestSupport() {}

    public static MockHttpServletRequestBuilder transfersRequest(String sku) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(transfersPath(sku));
    }

    public static MockHttpServletRequestBuilder transfersRequest(String sku, Integer page, Integer size) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(transfersPath(sku), page, size);
    }

    public static MockHttpServletRequestBuilder transfersRequestDefault(String sku) {
        return transfersRequest(sku, DEFAULT_PAGE, DEFAULT_SIZE);
    }

    public static MockHttpServletRequestBuilder transfersRequestDefault(String sku, String... optionalNameValuePairs) {
        return transfersRequest(sku, DEFAULT_PAGE, DEFAULT_SIZE, optionalNameValuePairs);
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
