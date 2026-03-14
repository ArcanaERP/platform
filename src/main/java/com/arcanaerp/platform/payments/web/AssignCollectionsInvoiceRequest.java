package com.arcanaerp.platform.payments.web;

import jakarta.validation.constraints.NotBlank;

public record AssignCollectionsInvoiceRequest(
    @NotBlank String assignedTo,
    @NotBlank String assignedBy
) {
}
