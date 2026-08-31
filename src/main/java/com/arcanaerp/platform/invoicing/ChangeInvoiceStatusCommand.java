package com.arcanaerp.platform.invoicing;

public record ChangeInvoiceStatusCommand(
    String invoiceNumber,
    InvoiceStatus status,
    String reason,
    String changedBy
) {
}
