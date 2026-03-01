package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.AdjustInventoryCommand;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryAdjustmentView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final String DEFAULT_LOCATION_CODE = "MAIN";

    private final InventoryAvailability inventoryAvailability;

    @GetMapping("/{sku}")
    public InventoryItemResponse inventoryBySku(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode
    ) {
        InventoryItemView item = inventoryAvailability.inventoryForSku(sku, normalizeOptionalLocationCode(locationCode));
        return new InventoryItemResponse(
            item.id(),
            item.sku(),
            item.locationCode(),
            item.onHandQuantity(),
            item.updatedAt()
        );
    }

    @GetMapping("/{sku}/adjustments")
    public PageResult<InventoryAdjustmentResponse> listAdjustments(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        String normalizedAdjustedBy = normalizeOptionalAdjustedBy(adjustedBy);
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listAdjustments(
                sku,
                normalizeOptionalLocationCode(locationCode),
                normalizedAdjustedBy,
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toAdjustmentResponse);
    }

    @PostMapping("/{sku}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryAdjustmentResponse adjustInventory(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @Valid @RequestBody AdjustInventoryRequest request
    ) {
        InventoryAdjustmentView adjustment = inventoryAvailability.adjustInventory(
            new AdjustInventoryCommand(
                sku,
                normalizeOptionalLocationCode(locationCode),
                request.quantityDelta(),
                request.reason(),
                request.adjustedBy()
            )
        );
        return toAdjustmentResponse(adjustment);
    }

    private InventoryAdjustmentResponse toAdjustmentResponse(InventoryAdjustmentView adjustment) {
        return new InventoryAdjustmentResponse(
            adjustment.id(),
            adjustment.sku(),
            adjustment.locationCode(),
            adjustment.previousOnHandQuantity(),
            adjustment.quantityDelta(),
            adjustment.currentOnHandQuantity(),
            adjustment.reason(),
            adjustment.adjustedBy(),
            adjustment.adjustedAt()
        );
    }

    private static String normalizeOptionalLocationCode(String locationCode) {
        if (locationCode == null) {
            return DEFAULT_LOCATION_CODE;
        }
        if (locationCode.isBlank()) {
            throw new IllegalArgumentException("locationCode query parameter must not be blank");
        }
        return locationCode.trim().toUpperCase();
    }

    private static String normalizeOptionalAdjustedBy(String adjustedBy) {
        if (adjustedBy == null) {
            return null;
        }
        if (adjustedBy.isBlank()) {
            throw new IllegalArgumentException("adjustedBy query parameter must not be blank");
        }
        return adjustedBy.trim().toLowerCase();
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be a valid ISO-8601 instant");
        }
    }

    private static void validateAdjustedAtRange(Instant adjustedAtFrom, Instant adjustedAtTo) {
        if (adjustedAtFrom != null && adjustedAtTo != null && adjustedAtFrom.isAfter(adjustedAtTo)) {
            throw new IllegalArgumentException("adjustedAtFrom must be before or equal to adjustedAtTo");
        }
    }
}
