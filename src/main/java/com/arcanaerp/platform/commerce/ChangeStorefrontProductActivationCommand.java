package com.arcanaerp.platform.commerce;

public record ChangeStorefrontProductActivationCommand(
    String tenantCode,
    String storefrontCode,
    String sku,
    boolean active,
    String reason,
    String changedBy
) {
}
