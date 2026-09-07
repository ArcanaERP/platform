package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFacilityDirectory;
import com.arcanaerp.platform.inventory.InventoryFacilityView;
import com.arcanaerp.platform.inventory.RegisterInventoryFacilityCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFacilityResponse createFacility(@Valid @RequestBody CreateInventoryFacilityRequest request) {
        return toResponse(inventoryFacilityDirectory.registerFacility(
            new RegisterInventoryFacilityCommand(
                request.code(),
                request.name(),
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

    @GetMapping("/{code}")
    public InventoryFacilityResponse facilityByCode(@PathVariable String code) {
        return toResponse(inventoryFacilityDirectory.facilityByCode(code));
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
            facility.addressLine1(),
            facility.addressLine2(),
            facility.city(),
            facility.regionCode(),
            facility.postalCode(),
            facility.countryCode(),
            facility.contactName(),
            facility.contactEmail(),
            facility.active(),
            facility.createdAt(),
            facility.updatedAt()
        );
    }
}
