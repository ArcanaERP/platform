package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record ReverseTransferRequest(
    @NotBlank String reason,
    @NotBlank String adjustedBy
) {
}
