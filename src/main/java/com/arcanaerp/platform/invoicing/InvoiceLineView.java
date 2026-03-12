package com.arcanaerp.platform.invoicing;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineView(
    UUID id,
    int lineNo,
    String productSku,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {
}
