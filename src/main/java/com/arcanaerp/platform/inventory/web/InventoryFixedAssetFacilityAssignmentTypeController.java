package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetFacilityAssignmentTypeCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFixedAssetFacilityAssignmentTypeMetadataCommand;
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
@RequestMapping("/api/inventory/fixed-asset-facility-assignment-types")
@RequiredArgsConstructor
public class InventoryFixedAssetFacilityAssignmentTypeController {

    private final InventoryFixedAssetFacilityAssignmentTypeDirectory assignmentTypeDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFixedAssetFacilityAssignmentTypeResponse createAssignmentType(
        @Valid @RequestBody CreateInventoryFixedAssetFacilityAssignmentTypeRequest request
    ) {
        return toResponse(assignmentTypeDirectory.registerAssignmentType(
            new RegisterInventoryFixedAssetFacilityAssignmentTypeCommand(request.code(), request.description())
        ));
    }

    @GetMapping("/{code}")
    public InventoryFixedAssetFacilityAssignmentTypeResponse assignmentTypeByCode(@PathVariable String code) {
        return toResponse(assignmentTypeDirectory.assignmentTypeByCode(code));
    }

    @PatchMapping("/{code}/metadata")
    public InventoryFixedAssetFacilityAssignmentTypeResponse updateAssignmentTypeMetadata(
        @PathVariable String code,
        @Valid @RequestBody UpdateInventoryFixedAssetFacilityAssignmentTypeMetadataRequest request
    ) {
        return toResponse(assignmentTypeDirectory.updateAssignmentTypeMetadata(
            code,
            new UpdateInventoryFixedAssetFacilityAssignmentTypeMetadataCommand(
                request.code(),
                request.description(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{code}/metadata-history")
    public PageResult<InventoryFixedAssetFacilityAssignmentTypeMetadataChangeResponse> listMetadataHistory(
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
        return assignmentTypeDirectory.listMetadataHistory(
            code,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping
    public PageResult<InventoryFixedAssetFacilityAssignmentTypeResponse> listAssignmentTypes(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return assignmentTypeDirectory.listAssignmentTypes(PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryFixedAssetFacilityAssignmentTypeResponse toResponse(
        InventoryFixedAssetFacilityAssignmentTypeView type
    ) {
        return new InventoryFixedAssetFacilityAssignmentTypeResponse(
            type.id(),
            type.code(),
            type.description(),
            type.createdAt(),
            type.updatedAt()
        );
    }

    private InventoryFixedAssetFacilityAssignmentTypeMetadataChangeResponse toMetadataChangeResponse(
        InventoryFixedAssetFacilityAssignmentTypeMetadataChangeView change
    ) {
        return new InventoryFixedAssetFacilityAssignmentTypeMetadataChangeResponse(
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
