package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryItemDirectory;
import com.arcanaerp.platform.inventory.InventoryItemAvailabilityChangeView;
import com.arcanaerp.platform.inventory.InventoryItemMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import com.arcanaerp.platform.inventory.RegisterInventoryItemCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryItemAvailabilityCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryItemMetadataCommand;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/items")
@RequiredArgsConstructor
public class InventoryItemController {

    private final InventoryItemDirectory inventoryItemDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemResponse createItem(@Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryItemView item = inventoryItemDirectory.registerItem(new RegisterInventoryItemCommand(
            request.sku(),
            request.locationCode(),
            request.onHandQuantity(),
            request.availableQuantity(),
            request.soldQuantity(),
            request.unitOfMeasurementCode(),
            request.classificationCode(),
            request.productInstanceCode(),
            request.externalReference(),
            request.sourceSystemCode()
        ));
        return toResponse(item);
    }

    @GetMapping("/{sku}/locations/{locationCode}")
    public InventoryItemResponse itemBySkuAndLocation(
        @PathVariable String sku,
        @PathVariable String locationCode
    ) {
        return toResponse(inventoryItemDirectory.itemBySkuAndLocation(sku, locationCode));
    }

    @PatchMapping("/{sku}/locations/{locationCode}/metadata")
    public InventoryItemResponse updateItemMetadata(
        @PathVariable String sku,
        @PathVariable String locationCode,
        @Valid @RequestBody UpdateInventoryItemMetadataRequest request
    ) {
        return toResponse(inventoryItemDirectory.updateItemMetadata(
            sku,
            locationCode,
            new UpdateInventoryItemMetadataCommand(
                sku,
                locationCode,
                request.unitOfMeasurementCode(),
                request.classificationCode(),
                request.productInstanceCode(),
                request.externalReference(),
                request.sourceSystemCode(),
                request.changedBy()
            )
        ));
    }

    @PatchMapping("/{sku}/locations/{locationCode}/availability")
    public InventoryItemResponse updateItemAvailability(
        @PathVariable String sku,
        @PathVariable String locationCode,
        @Valid @RequestBody UpdateInventoryItemAvailabilityRequest request
    ) {
        return toResponse(inventoryItemDirectory.updateItemAvailability(
            sku,
            locationCode,
            new UpdateInventoryItemAvailabilityCommand(
                sku,
                locationCode,
                request.availableQuantityDelta(),
                request.soldQuantityDelta(),
                request.reason(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{sku}/locations/{locationCode}/availability-history")
    public PageResult<InventoryItemAvailabilityChangeResponse> listAvailabilityHistory(
        @PathVariable String sku,
        @PathVariable String locationCode,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return inventoryItemDirectory.listAvailabilityHistory(
            sku,
            locationCode,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toAvailabilityChangeResponse);
    }

    @GetMapping("/{sku}/locations/{locationCode}/metadata-history")
    public PageResult<InventoryItemMetadataChangeResponse> listMetadataHistory(
        @PathVariable String sku,
        @PathVariable String locationCode,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return inventoryItemDirectory.listMetadataHistory(
            sku,
            locationCode,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping
    public PageResult<InventoryItemResponse> listItems(
        @RequestParam(required = false) String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String unitOfMeasurementCode,
        @RequestParam(required = false) String classificationCode,
        @RequestParam(required = false) String productInstanceCode,
        @RequestParam(required = false) String externalReference,
        @RequestParam(required = false) String sourceSystemCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryItemDirectory.listItems(
            sku,
            locationCode,
            unitOfMeasurementCode,
            classificationCode,
            productInstanceCode,
            externalReference,
            sourceSystemCode,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryItemResponse toResponse(InventoryItemView item) {
        return new InventoryItemResponse(
            item.id(),
            item.sku(),
            item.locationCode(),
            item.onHandQuantity(),
            item.availableQuantity(),
            item.soldQuantity(),
            item.unitOfMeasurementCode(),
            item.classificationCode(),
            item.productInstanceCode(),
            item.externalReference(),
            item.sourceSystemCode(),
            item.updatedAt()
        );
    }

    private InventoryItemAvailabilityChangeResponse toAvailabilityChangeResponse(InventoryItemAvailabilityChangeView change) {
        return new InventoryItemAvailabilityChangeResponse(
            change.id(),
            change.sku(),
            change.locationCode(),
            change.previousAvailableQuantity(),
            change.currentAvailableQuantity(),
            change.availableQuantityDelta(),
            change.previousSoldQuantity(),
            change.currentSoldQuantity(),
            change.soldQuantityDelta(),
            change.reason(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private InventoryItemMetadataChangeResponse toMetadataChangeResponse(InventoryItemMetadataChangeView change) {
        return new InventoryItemMetadataChangeResponse(
            change.id(),
            change.sku(),
            change.locationCode(),
            change.previousUnitOfMeasurementCode(),
            change.currentUnitOfMeasurementCode(),
            change.previousClassificationCode(),
            change.currentClassificationCode(),
            change.previousProductInstanceCode(),
            change.currentProductInstanceCode(),
            change.previousExternalReference(),
            change.currentExternalReference(),
            change.previousSourceSystemCode(),
            change.currentSourceSystemCode(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private static String normalizeOptionalChangedBy(String changedBy) {
        if (changedBy == null) {
            return null;
        }
        if (changedBy.isBlank()) {
            throw new IllegalArgumentException("changedBy query parameter must not be blank");
        }
        return changedBy.trim().toLowerCase();
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

    private static void validateChangedAtRange(Instant changedAtFrom, Instant changedAtTo) {
        if (changedAtFrom != null && changedAtTo != null && changedAtFrom.isAfter(changedAtTo)) {
            throw new IllegalArgumentException("changedAtFrom must be before or equal to changedAtTo");
        }
    }
}
