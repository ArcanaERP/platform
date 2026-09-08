package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryPostalAddressDirectory;
import com.arcanaerp.platform.inventory.InventoryPostalAddressMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryPostalAddressView;
import com.arcanaerp.platform.inventory.RegisterInventoryPostalAddressCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryPostalAddressMetadataCommand;
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
@RequestMapping("/api/inventory/postal-addresses")
@RequiredArgsConstructor
public class InventoryPostalAddressController {

    private final InventoryPostalAddressDirectory inventoryPostalAddressDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryPostalAddressResponse createPostalAddress(
        @Valid @RequestBody CreateInventoryPostalAddressRequest request
    ) {
        return toResponse(inventoryPostalAddressDirectory.registerPostalAddress(
            new RegisterInventoryPostalAddressCommand(
                request.ownerType(),
                request.ownerCode(),
                request.addressPurposeCode(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.regionCode(),
                request.postalCode(),
                request.countryCode()
            )
        ));
    }

    @GetMapping("/{id}")
    public InventoryPostalAddressResponse postalAddressById(@PathVariable UUID id) {
        return toResponse(inventoryPostalAddressDirectory.postalAddressById(id));
    }

    @PatchMapping("/{id}/metadata")
    public InventoryPostalAddressResponse updatePostalAddressMetadata(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInventoryPostalAddressMetadataRequest request
    ) {
        return toResponse(inventoryPostalAddressDirectory.updatePostalAddressMetadata(
            id,
            new UpdateInventoryPostalAddressMetadataCommand(
                request.addressPurposeCode(),
                request.addressLine1(),
                request.addressLine2(),
                request.city(),
                request.regionCode(),
                request.postalCode(),
                request.countryCode(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{id}/metadata-history")
    public PageResult<InventoryPostalAddressMetadataChangeResponse> listMetadataHistory(
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
        return inventoryPostalAddressDirectory.listMetadataHistory(
            id,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping
    public PageResult<InventoryPostalAddressResponse> listPostalAddresses(
        @RequestParam(required = false) String ownerType,
        @RequestParam(required = false) String ownerCode,
        @RequestParam(required = false) String addressPurposeCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryPostalAddressDirectory.listPostalAddresses(
            ownerType,
            ownerCode,
            addressPurposeCode,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryPostalAddressResponse toResponse(InventoryPostalAddressView address) {
        return new InventoryPostalAddressResponse(
            address.id(),
            address.ownerType(),
            address.ownerCode(),
            address.addressPurposeCode(),
            address.addressLine1(),
            address.addressLine2(),
            address.city(),
            address.regionCode(),
            address.postalCode(),
            address.countryCode(),
            address.createdAt(),
            address.updatedAt()
        );
    }

    private InventoryPostalAddressMetadataChangeResponse toMetadataChangeResponse(
        InventoryPostalAddressMetadataChangeView change
    ) {
        return new InventoryPostalAddressMetadataChangeResponse(
            change.id(),
            change.postalAddressId(),
            change.ownerType(),
            change.ownerCode(),
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
