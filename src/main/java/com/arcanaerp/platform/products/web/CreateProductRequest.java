package com.arcanaerp.platform.products.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank String sku,
    @NotBlank String name,
    @NotBlank String categoryCode,
    @NotBlank String categoryName,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank @Pattern(regexp = "(?i)^[A-Z]{3}$") String currencyCode
) {
}
