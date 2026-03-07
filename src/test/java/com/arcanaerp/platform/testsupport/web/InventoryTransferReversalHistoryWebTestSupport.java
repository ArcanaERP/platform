package com.arcanaerp.platform.testsupport.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import java.util.UUID;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class InventoryTransferReversalHistoryWebTestSupport {

    private static final int DEFAULT_PAGE = PageQuery.DEFAULT_PAGE;
    private static final int DEFAULT_SIZE = PageQuery.DEFAULT_SIZE;

    private InventoryTransferReversalHistoryWebTestSupport() {}

    public static MockHttpServletRequestBuilder reversalsRequest(UUID transferId) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(reversalsPath(transferId));
    }

    public static MockHttpServletRequestBuilder reversalsRequest(UUID transferId, Integer page, Integer size) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(reversalsPath(transferId), page, size);
    }

    public static MockHttpServletRequestBuilder reversalsRequestDefault(UUID transferId) {
        return reversalsRequest(transferId, DEFAULT_PAGE, DEFAULT_SIZE);
    }

    public static MockHttpServletRequestBuilder reversalsRequest(
        UUID transferId,
        Integer page,
        Integer size,
        String... optionalNameValuePairs
    ) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(
            reversalsPath(transferId),
            page,
            size,
            optionalNameValuePairs
        );
    }

    private static String reversalsPath(UUID transferId) {
        return "/api/inventory/transfers/" + transferId + "/reversals";
    }
}
