package com.arcanaerp.platform.products;

public record ChangeProductActivationCommand(
    String sku,
    boolean active
) {
}
