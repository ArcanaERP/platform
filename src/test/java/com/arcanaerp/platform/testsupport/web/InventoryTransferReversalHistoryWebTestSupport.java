package com.arcanaerp.platform.testsupport.web;

import java.util.UUID;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class InventoryTransferReversalHistoryWebTestSupport {

    private InventoryTransferReversalHistoryWebTestSupport() {}

    public static MockHttpServletRequestBuilder reversalsRequest(UUID transferId) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(reversalsPath(transferId));
    }

    public static MockHttpServletRequestBuilder reversalsRequest(UUID transferId, Integer page, Integer size) {
        return StatusHistoryWebTestSupport.statusHistoryRequest(reversalsPath(transferId), page, size);
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
