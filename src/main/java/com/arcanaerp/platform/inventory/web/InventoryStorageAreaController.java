package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryStorageAreaDirectory;
import com.arcanaerp.platform.inventory.InventoryStorageAreaActiveChangeView;
import com.arcanaerp.platform.inventory.InventoryStorageAreaMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryStorageAreaView;
import com.arcanaerp.platform.inventory.RegisterInventoryStorageAreaCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryStorageAreaActiveCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryStorageAreaMetadataCommand;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
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
@RequestMapping("/api/inventory/storage-areas")
@RequiredArgsConstructor
public class InventoryStorageAreaController {

    private final InventoryStorageAreaDirectory inventoryStorageAreaDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryStorageAreaResponse createStorageArea(
        @Valid @RequestBody CreateInventoryStorageAreaRequest request
    ) {
        return toResponse(inventoryStorageAreaDirectory.registerStorageArea(new RegisterInventoryStorageAreaCommand(
            request.facilityCode(),
            request.code(),
            request.name(),
            request.storageAreaType(),
            request.parentStorageAreaCode()
        )));
    }

    @GetMapping("/{id}")
    public InventoryStorageAreaResponse storageAreaById(@PathVariable UUID id) {
        return toResponse(inventoryStorageAreaDirectory.storageAreaById(id));
    }

    @PatchMapping("/{id}/active")
    public InventoryStorageAreaResponse updateStorageAreaActive(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInventoryStorageAreaActiveRequest request
    ) {
        return toResponse(inventoryStorageAreaDirectory.updateStorageAreaActive(
            id,
            new UpdateInventoryStorageAreaActiveCommand(request.active(), request.changedBy())
        ));
    }

    @PatchMapping("/{id}/metadata")
    public InventoryStorageAreaResponse updateStorageAreaMetadata(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInventoryStorageAreaMetadataRequest request
    ) {
        return toResponse(inventoryStorageAreaDirectory.updateStorageAreaMetadata(
            id,
            new UpdateInventoryStorageAreaMetadataCommand(
                request.name(),
                request.storageAreaType(),
                request.parentStorageAreaCode(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{id}/active-history")
    public PageResult<InventoryStorageAreaActiveChangeResponse> listActiveHistory(
        @PathVariable UUID id,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return inventoryStorageAreaDirectory.listActiveHistory(
            id,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toActiveChangeResponse);
    }

    @GetMapping("/{id}/metadata-history")
    public PageResult<InventoryStorageAreaMetadataChangeResponse> listMetadataHistory(
        @PathVariable UUID id,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return inventoryStorageAreaDirectory.listMetadataHistory(
            id,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping
    public PageResult<InventoryStorageAreaResponse> listStorageAreas(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String facilityCode,
        @RequestParam(required = false) String storageAreaType,
        @RequestParam(required = false) String parentStorageAreaCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryStorageAreaDirectory.listStorageAreas(
            active,
            facilityCode,
            storageAreaType,
            parentStorageAreaCode,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryStorageAreaResponse toResponse(InventoryStorageAreaView storageArea) {
        return new InventoryStorageAreaResponse(
            storageArea.id(),
            storageArea.facilityCode(),
            storageArea.code(),
            storageArea.name(),
            storageArea.storageAreaType(),
            storageArea.parentStorageAreaCode(),
            storageArea.active(),
            storageArea.createdAt(),
            storageArea.updatedAt()
        );
    }

    private InventoryStorageAreaActiveChangeResponse toActiveChangeResponse(
        InventoryStorageAreaActiveChangeView change
    ) {
        return new InventoryStorageAreaActiveChangeResponse(
            change.id(),
            change.storageAreaId(),
            change.facilityCode(),
            change.storageAreaCode(),
            change.previousActive(),
            change.currentActive(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private InventoryStorageAreaMetadataChangeResponse toMetadataChangeResponse(
        InventoryStorageAreaMetadataChangeView change
    ) {
        return new InventoryStorageAreaMetadataChangeResponse(
            change.id(),
            change.storageAreaId(),
            change.facilityCode(),
            change.storageAreaCode(),
            change.previousName(),
            change.currentName(),
            change.previousStorageAreaType(),
            change.currentStorageAreaType(),
            change.previousParentStorageAreaCode(),
            change.currentParentStorageAreaCode(),
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
