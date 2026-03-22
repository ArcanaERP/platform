package com.arcanaerp.platform.payments;

public record ReleaseCollectionsInvoiceCommand(
    String tenantCode,
    String invoiceNumber,
    String releasedBy
) {
}
