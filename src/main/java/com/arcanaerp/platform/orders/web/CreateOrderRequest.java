package com.arcanaerp.platform.orders.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreateOrderRequest(
    @NotBlank String orderNumber,
    @NotBlank @Email String customerEmail,
    @NotBlank @Pattern(regexp = "(?i)^[A-Z]{3}$") String currencyCode,
    @NotEmpty List<@Valid CreateOrderLineRequest> lines
) {
}
