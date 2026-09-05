package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryLocationDirectory;
import com.arcanaerp.platform.inventory.InventoryLocationView;
import com.arcanaerp.platform.inventory.RegisterInventoryLocationCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryLocationActiveCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryLocationMetadataCommand;
import jakarta.validation.Valid;
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
                request.contactEmail()
            )
        ));
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
}
