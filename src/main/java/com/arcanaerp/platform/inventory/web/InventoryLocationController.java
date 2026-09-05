package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryLocationDirectory;
import com.arcanaerp.platform.inventory.InventoryLocationMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryLocationView;
import com.arcanaerp.platform.inventory.RegisterInventoryLocationCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryLocationActiveCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryLocationMetadataCommand;
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
@RequestMapping("/api/inventory/locations")
@RequiredArgsConstructor
public class InventoryLocationController {

    private final InventoryLocationDirectory inventoryLocationDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryLocationResponse createLocation(@Valid @RequestBody CreateInventoryLocationRequest request) {
        InventoryLocationView location = inventoryLocationDirectory.registerLocation(
            new RegisterInventoryLocationCommand(
                request.code(),
                request.name(),
                request.facilityTypeCode(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.regionCode(),
                request.postalCode(),
                request.countryCode(),
                request.contactName(),
                request.contactEmail()
            )
        );
        return toResponse(location);
    }

    @GetMapping("/{code}")
    public InventoryLocationResponse locationByCode(@PathVariable String code) {
        return toResponse(inventoryLocationDirectory.locationByCode(code));
    }

    @PatchMapping("/{code}/active")
    public InventoryLocationResponse updateLocationActive(
        @PathVariable String code,
        @Valid @RequestBody UpdateInventoryLocationActiveRequest request
    ) {
        return toResponse(inventoryLocationDirectory.updateLocationActive(
            code,
            new UpdateInventoryLocationActiveCommand(code, request.active())
        ));
    }

    @PatchMapping("/{code}/metadata")
    public InventoryLocationResponse updateLocationMetadata(
        @PathVariable String code,
        @Valid @RequestBody UpdateInventoryLocationMetadataRequest request
    ) {
        return toResponse(inventoryLocationDirectory.updateLocationMetadata(
            code,
            new UpdateInventoryLocationMetadataCommand(
                code,
                request.name(),
                request.facilityTypeCode(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.regionCode(),
                request.postalCode(),
                request.countryCode(),
                request.contactName(),
                request.contactEmail(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{code}/metadata-history")
    public PageResult<InventoryLocationMetadataChangeResponse> listMetadataHistory(
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
        return inventoryLocationDirectory.listMetadataHistory(
            code,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping
    public PageResult<InventoryLocationResponse> listLocations(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryLocationDirectory.listLocations(active, PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryLocationResponse toResponse(InventoryLocationView location) {
        return new InventoryLocationResponse(
            location.id(),
            location.code(),
            location.name(),
            location.facilityTypeCode(),
            location.addressLine1(),
            location.addressLine2(),
            location.city(),
            location.regionCode(),
            location.postalCode(),
            location.countryCode(),
            location.contactName(),
            location.contactEmail(),
            location.active(),
            location.createdAt(),
            location.updatedAt()
        );
    }

    private InventoryLocationMetadataChangeResponse toMetadataChangeResponse(InventoryLocationMetadataChangeView change) {
        return new InventoryLocationMetadataChangeResponse(
            change.id(),
            change.locationCode(),
            change.previousName(),
            change.currentName(),
            change.previousFacilityTypeCode(),
            change.currentFacilityTypeCode(),
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
