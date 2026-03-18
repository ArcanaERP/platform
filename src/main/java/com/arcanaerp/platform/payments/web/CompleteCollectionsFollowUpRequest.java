package com.arcanaerp.platform.payments.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CompleteCollectionsFollowUpRequest(
    @NotBlank @Email String completedBy,
    @NotBlank String outcome
) {
}
