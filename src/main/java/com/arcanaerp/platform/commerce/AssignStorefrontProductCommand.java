package com.arcanaerp.platform.commerce;

public record AssignStorefrontProductCommand(
    String tenantCode,
    String storefrontCode,
    String sku,
    String merchandisingName,
    int position,
    boolean active
) {
}
