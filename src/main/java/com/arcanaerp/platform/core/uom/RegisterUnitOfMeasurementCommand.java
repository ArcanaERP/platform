package com.arcanaerp.platform.core.uom;

public record RegisterUnitOfMeasurementCommand(
    String code,
    String description,
    String domain,
    String comments
) {
}
