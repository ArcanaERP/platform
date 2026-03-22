package com.arcanaerp.platform.payments;

public record ClaimCollectionsInvoiceCommand(
    String tenantCode,
    String invoiceNumber,
    String claimedBy
) {
}
