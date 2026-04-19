package com.arcanaerp.platform.communicationevents.web;

import jakarta.validation.constraints.NotBlank;

public record ChangeCommunicationEventStatusRequest(
    @NotBlank String tenantCode,
    @NotBlank String statusCode,
    @NotBlank String reason,
    @NotBlank String changedBy
) {
}
