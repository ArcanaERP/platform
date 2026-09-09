package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetTypeCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFixedAssetTypeMetadataCommand;
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
@RequestMapping("/api/inventory/fixed-asset-types")
@RequiredArgsConstructor
public class InventoryFixedAssetTypeController {

    private final InventoryFixedAssetTypeDirectory inventoryFixedAssetTypeDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFixedAssetTypeResponse createFixedAssetType(
        @Valid @RequestBody CreateInventoryFixedAssetTypeRequest request
    ) {
        return toResponse(inventoryFixedAssetTypeDirectory.registerFixedAssetType(
            new RegisterInventoryFixedAssetTypeCommand(request.code(), request.description())
        ));
    }

    @GetMapping("/{code}")
    public InventoryFixedAssetTypeResponse fixedAssetTypeByCode(@PathVariable String code) {
        return toResponse(inventoryFixedAssetTypeDirectory.fixedAssetTypeByCode(code));
    }

    @PatchMapping("/{code}/metadata")
    public InventoryFixedAssetTypeResponse updateFixedAssetTypeMetadata(
        @PathVariable String code,
        @Valid @RequestBody UpdateInventoryFixedAssetTypeMetadataRequest request
    ) {
        return toResponse(inventoryFixedAssetTypeDirectory.updateFixedAssetTypeMetadata(
            code,
            new UpdateInventoryFixedAssetTypeMetadataCommand(
                request.code(),
                request.description(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{code}/metadata-history")
    public PageResult<InventoryFixedAssetTypeMetadataChangeResponse> listMetadataHistory(
        @PathVariable String code,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return inventoryFixedAssetTypeDirectory.listMetadataHistory(
            code,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping
    public PageResult<InventoryFixedAssetTypeResponse> listFixedAssetTypes(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryFixedAssetTypeDirectory.listFixedAssetTypes(PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryFixedAssetTypeResponse toResponse(InventoryFixedAssetTypeView type) {
        return new InventoryFixedAssetTypeResponse(
            type.id(),
            type.code(),
            type.description(),
            type.createdAt(),
            type.updatedAt()
        );
    }

    private InventoryFixedAssetTypeMetadataChangeResponse toMetadataChangeResponse(
        InventoryFixedAssetTypeMetadataChangeView change
    ) {
        return new InventoryFixedAssetTypeMetadataChangeResponse(
            change.id(),
            change.code(),
            change.previousDescription(),
            change.currentDescription(),
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
