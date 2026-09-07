package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetActiveChangeView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFixedAssetActiveCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFixedAssetMetadataCommand;
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
@RequestMapping("/api/inventory/fixed-assets")
@RequiredArgsConstructor
public class InventoryFixedAssetController {

    private final InventoryFixedAssetDirectory inventoryFixedAssetDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFixedAssetResponse createFixedAsset(@Valid @RequestBody CreateInventoryFixedAssetRequest request) {
        return toResponse(inventoryFixedAssetDirectory.registerFixedAsset(
            new RegisterInventoryFixedAssetCommand(
                request.code(),
                request.description(),
                request.fixedAssetTypeCode(),
                request.comments(),
                request.externalIdentifier(),
                request.externalIdSource()
            )
        ));
    }

    @GetMapping("/{code}")
    public InventoryFixedAssetResponse fixedAssetByCode(@PathVariable String code) {
        return toResponse(inventoryFixedAssetDirectory.fixedAssetByCode(code));
    }

    @PatchMapping("/{code}/active")
    public InventoryFixedAssetResponse updateFixedAssetActive(
        @PathVariable String code,
        @Valid @RequestBody UpdateInventoryFixedAssetActiveRequest request
    ) {
        return toResponse(inventoryFixedAssetDirectory.updateFixedAssetActive(
            code,
            new UpdateInventoryFixedAssetActiveCommand(code, request.active(), request.changedBy())
        ));
    }

    @PatchMapping("/{code}/metadata")
    public InventoryFixedAssetResponse updateFixedAssetMetadata(
        @PathVariable String code,
        @Valid @RequestBody UpdateInventoryFixedAssetMetadataRequest request
    ) {
        return toResponse(inventoryFixedAssetDirectory.updateFixedAssetMetadata(
            code,
            new UpdateInventoryFixedAssetMetadataCommand(
                code,
                request.description(),
                request.fixedAssetTypeCode(),
                request.comments(),
                request.externalIdentifier(),
                request.externalIdSource(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{code}/active-history")
    public PageResult<InventoryFixedAssetActiveChangeResponse> listActiveHistory(
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
        return inventoryFixedAssetDirectory.listActiveHistory(
            code,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toActiveChangeResponse);
    }

    @GetMapping("/{code}/metadata-history")
    public PageResult<InventoryFixedAssetMetadataChangeResponse> listMetadataHistory(
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
        return inventoryFixedAssetDirectory.listMetadataHistory(
            code,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping
    public PageResult<InventoryFixedAssetResponse> listFixedAssets(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryFixedAssetDirectory.listFixedAssets(active, query, PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryFixedAssetResponse toResponse(InventoryFixedAssetView fixedAsset) {
        return new InventoryFixedAssetResponse(
            fixedAsset.id(),
            fixedAsset.code(),
            fixedAsset.description(),
            fixedAsset.fixedAssetTypeCode(),
            fixedAsset.comments(),
            fixedAsset.externalIdentifier(),
            fixedAsset.externalIdSource(),
            fixedAsset.active(),
            fixedAsset.createdAt(),
            fixedAsset.updatedAt()
        );
    }

    private InventoryFixedAssetActiveChangeResponse toActiveChangeResponse(InventoryFixedAssetActiveChangeView change) {
        return new InventoryFixedAssetActiveChangeResponse(
            change.id(),
            change.fixedAssetCode(),
            change.previousActive(),
            change.currentActive(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private InventoryFixedAssetMetadataChangeResponse toMetadataChangeResponse(InventoryFixedAssetMetadataChangeView change) {
        return new InventoryFixedAssetMetadataChangeResponse(
            change.id(),
            change.fixedAssetCode(),
            change.previousDescription(),
            change.currentDescription(),
            change.previousFixedAssetTypeCode(),
            change.currentFixedAssetTypeCode(),
            change.previousComments(),
            change.currentComments(),
            change.previousExternalIdentifier(),
            change.currentExternalIdentifier(),
            change.previousExternalIdSource(),
            change.currentExternalIdSource(),
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
