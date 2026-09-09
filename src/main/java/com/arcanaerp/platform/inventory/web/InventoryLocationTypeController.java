package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryLocationTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryLocationTypeMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryLocationTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryLocationTypeCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryLocationTypeMetadataCommand;
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
@RequestMapping("/api/inventory/location-types")
@RequiredArgsConstructor
public class InventoryLocationTypeController {

    private final InventoryLocationTypeDirectory inventoryLocationTypeDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryLocationTypeResponse createLocationType(@Valid @RequestBody CreateInventoryLocationTypeRequest request) {
        return toResponse(inventoryLocationTypeDirectory.registerLocationType(
            new RegisterInventoryLocationTypeCommand(request.code(), request.description(), request.parentCode())
        ));
    }

    @GetMapping("/{code}")
    public InventoryLocationTypeResponse locationTypeByCode(@PathVariable String code) {
        return toResponse(inventoryLocationTypeDirectory.locationTypeByCode(code));
    }

    @PatchMapping("/{code}/metadata")
    public InventoryLocationTypeResponse updateLocationTypeMetadata(
        @PathVariable String code,
        @Valid @RequestBody UpdateInventoryLocationTypeMetadataRequest request
    ) {
        return toResponse(inventoryLocationTypeDirectory.updateLocationTypeMetadata(
            code,
            new UpdateInventoryLocationTypeMetadataCommand(
                request.code(),
                request.description(),
                request.parentCode(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{code}/metadata-history")
    public PageResult<InventoryLocationTypeMetadataChangeResponse> listMetadataHistory(
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
        return inventoryLocationTypeDirectory.listMetadataHistory(
            code,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping
    public PageResult<InventoryLocationTypeResponse> listLocationTypes(
        @RequestParam(required = false) String parentCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryLocationTypeDirectory.listLocationTypes(parentCode, PageQuery.of(page, size))
            .map(this::toResponse);
    }

    private InventoryLocationTypeResponse toResponse(InventoryLocationTypeView type) {
        return new InventoryLocationTypeResponse(
            type.id(),
            type.code(),
            type.description(),
            type.parentCode(),
            type.createdAt(),
            type.updatedAt()
        );
    }

    private InventoryLocationTypeMetadataChangeResponse toMetadataChangeResponse(
        InventoryLocationTypeMetadataChangeView change
    ) {
        return new InventoryLocationTypeMetadataChangeResponse(
            change.id(),
            change.code(),
            change.previousDescription(),
            change.currentDescription(),
            change.previousParentCode(),
            change.currentParentCode(),
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
