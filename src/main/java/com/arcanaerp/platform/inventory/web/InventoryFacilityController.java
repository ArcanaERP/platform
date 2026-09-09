package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentView;
import com.arcanaerp.platform.inventory.InventoryFacilityDirectory;
import com.arcanaerp.platform.inventory.InventoryFacilityActiveChangeView;
import com.arcanaerp.platform.inventory.InventoryFacilityMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryFacilityView;
import com.arcanaerp.platform.inventory.RegisterInventoryFacilityCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFacilityActiveCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryFacilityMetadataCommand;
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
@RequestMapping("/api/inventory/facilities")
@RequiredArgsConstructor
public class InventoryFacilityController {

    private final InventoryFacilityDirectory inventoryFacilityDirectory;
    private final InventoryFixedAssetFacilityAssignmentDirectory fixedAssetFacilityAssignmentDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFacilityResponse createFacility(@Valid @RequestBody CreateInventoryFacilityRequest request) {
        return toResponse(inventoryFacilityDirectory.registerFacility(
            new RegisterInventoryFacilityCommand(
                request.code(),
                request.name(),
                request.facilityTypeCode(),
                request.addressPurposeCode(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.regionCode(),
                request.postalCode(),
                request.countryCode(),
                request.contactPurposeCode(),
                request.contactName(),
                request.contactEmail()
            )
        ));
    }

    @GetMapping("/{code}")
    public InventoryFacilityResponse facilityByCode(@PathVariable String code) {
        return toResponse(inventoryFacilityDirectory.facilityByCode(code));
    }

    @PatchMapping("/{code}/active")
    public InventoryFacilityResponse updateFacilityActive(
        @PathVariable String code,
        @Valid @RequestBody UpdateInventoryFacilityActiveRequest request
    ) {
        return toResponse(inventoryFacilityDirectory.updateFacilityActive(
            code,
            new UpdateInventoryFacilityActiveCommand(code, request.active(), request.changedBy())
        ));
    }

    @PatchMapping("/{code}/metadata")
    public InventoryFacilityResponse updateFacilityMetadata(
        @PathVariable String code,
        @Valid @RequestBody UpdateInventoryFacilityMetadataRequest request
    ) {
        return toResponse(inventoryFacilityDirectory.updateFacilityMetadata(
            code,
            new UpdateInventoryFacilityMetadataCommand(
                code,
                request.name(),
                request.facilityTypeCode(),
                request.addressPurposeCode(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.regionCode(),
                request.postalCode(),
                request.countryCode(),
                request.contactPurposeCode(),
                request.contactName(),
                request.contactEmail(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{code}/active-history")
    public PageResult<InventoryFacilityActiveChangeResponse> listActiveHistory(
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
        return inventoryFacilityDirectory.listActiveHistory(
            code,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toActiveChangeResponse);
    }

    @GetMapping("/{code}/metadata-history")
    public PageResult<InventoryFacilityMetadataChangeResponse> listMetadataHistory(
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
        return inventoryFacilityDirectory.listMetadataHistory(
            code,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping("/{code}/current-fixed-assets")
    public PageResult<InventoryFixedAssetFacilityAssignmentResponse> listCurrentFixedAssets(
        @PathVariable String code,
        @RequestParam(required = false) String assignmentType,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        inventoryFacilityDirectory.facilityByCode(code);
        return fixedAssetFacilityAssignmentDirectory.listCurrentAssignments(
            null,
            code,
            assignmentType,
            PageQuery.of(page, size)
        ).map(this::toFixedAssetFacilityAssignmentResponse);
    }

    @GetMapping
    public PageResult<InventoryFacilityResponse> listFacilities(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryFacilityDirectory.listFacilities(active, query, PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryFacilityResponse toResponse(InventoryFacilityView facility) {
        return new InventoryFacilityResponse(
            facility.id(),
            facility.code(),
            facility.name(),
            facility.facilityTypeCode(),
            facility.addressPurposeCode(),
            facility.addressLine1(),
            facility.addressLine2(),
            facility.city(),
            facility.regionCode(),
            facility.postalCode(),
            facility.countryCode(),
            facility.contactPurposeCode(),
            facility.contactName(),
            facility.contactEmail(),
            facility.active(),
            facility.createdAt(),
            facility.updatedAt()
        );
    }

    private InventoryFixedAssetFacilityAssignmentResponse toFixedAssetFacilityAssignmentResponse(
        InventoryFixedAssetFacilityAssignmentView assignment
    ) {
        return new InventoryFixedAssetFacilityAssignmentResponse(
            assignment.id(),
            assignment.inventoryFixedAssetId(),
            assignment.inventoryFacilityId(),
            assignment.fixedAssetCode(),
            assignment.facilityCode(),
            assignment.assignmentType(),
            assignment.comments(),
            assignment.fromDate(),
            assignment.thruDate(),
            assignment.assignedBy(),
            assignment.assignedAt(),
            assignment.active(),
            assignment.endReason(),
            assignment.endedBy(),
            assignment.endedAt()
        );
    }

    private InventoryFacilityActiveChangeResponse toActiveChangeResponse(InventoryFacilityActiveChangeView change) {
        return new InventoryFacilityActiveChangeResponse(
            change.id(),
            change.facilityCode(),
            change.previousActive(),
            change.currentActive(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private InventoryFacilityMetadataChangeResponse toMetadataChangeResponse(InventoryFacilityMetadataChangeView change) {
        return new InventoryFacilityMetadataChangeResponse(
            change.id(),
            change.facilityCode(),
            change.previousName(),
            change.currentName(),
            change.previousFacilityTypeCode(),
            change.currentFacilityTypeCode(),
            change.previousAddressPurposeCode(),
            change.currentAddressPurposeCode(),
            change.previousAddressLine1(),
            change.currentAddressLine1(),
            change.previousAddressLine2(),
            change.currentAddressLine2(),
            change.previousCity(),
            change.currentCity(),
            change.previousRegionCode(),
            change.currentRegionCode(),
            change.previousPostalCode(),
            change.currentPostalCode(),
            change.previousCountryCode(),
            change.currentCountryCode(),
            change.previousContactPurposeCode(),
            change.currentContactPurposeCode(),
            change.previousContactName(),
            change.currentContactName(),
            change.previousContactEmail(),
            change.currentContactEmail(),
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
