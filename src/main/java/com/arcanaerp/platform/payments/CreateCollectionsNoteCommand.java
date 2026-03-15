package com.arcanaerp.platform.payments;

public record CreateCollectionsNoteCommand(
    String tenantCode,
    String invoiceNumber,
    String note,
    String notedBy
) {
}
