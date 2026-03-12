package com.arcanaerp.platform.invoicing.web;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineResponse(
    UUID id,
    int lineNo,
    String productSku,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {
}
