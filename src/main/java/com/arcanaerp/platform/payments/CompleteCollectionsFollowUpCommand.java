package com.arcanaerp.platform.payments;

public record CompleteCollectionsFollowUpCommand(
    String tenantCode,
    String invoiceNumber,
    String completedBy,
    CollectionsFollowUpOutcome outcome
) {
}
