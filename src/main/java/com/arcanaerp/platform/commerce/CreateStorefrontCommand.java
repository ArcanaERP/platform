package com.arcanaerp.platform.commerce;

public record CreateStorefrontCommand(
    String tenantCode,
    String storefrontCode,
    String name,
    String currencyCode,
    String defaultLanguageTag,
    boolean active
) {
}
