package com.arcanaerp.platform.payments;

public record AssignCollectionsInvoiceCommand(
    String tenantCode,
    String invoiceNumber,
    String assignedTo,
    String assignedBy
) {
}
