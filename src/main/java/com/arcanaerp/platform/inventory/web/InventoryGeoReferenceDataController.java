package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryCountryDirectory;
import com.arcanaerp.platform.inventory.InventoryCountryView;
import com.arcanaerp.platform.inventory.InventoryRegionDirectory;
import com.arcanaerp.platform.inventory.InventoryRegionView;
import com.arcanaerp.platform.inventory.RegisterInventoryCountryCommand;
import com.arcanaerp.platform.inventory.RegisterInventoryRegionCommand;
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
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryGeoReferenceDataController {

    private final InventoryCountryDirectory countryDirectory;
    private final InventoryRegionDirectory regionDirectory;

    @PostMapping("/countries")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryCountryResponse createCountry(@Valid @RequestBody CreateInventoryCountryRequest request) {
        return toResponse(countryDirectory.registerCountry(
            new RegisterInventoryCountryCommand(request.code(), request.name())
        ));
    }

    @GetMapping("/countries/{code}")
    public InventoryCountryResponse countryByCode(@PathVariable String code) {
        return toResponse(countryDirectory.countryByCode(code));
    }

    @GetMapping("/countries")
    public PageResult<InventoryCountryResponse> listCountries(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return countryDirectory.listCountries(PageQuery.of(page, size)).map(this::toResponse);
    }

    @PostMapping("/regions")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryRegionResponse createRegion(@Valid @RequestBody CreateInventoryRegionRequest request) {
        return toResponse(regionDirectory.registerRegion(
            new RegisterInventoryRegionCommand(request.countryCode(), request.code(), request.name())
        ));
    }

    @GetMapping("/countries/{countryCode}/regions/{code}")
    public InventoryRegionResponse regionByCountryAndCode(
        @PathVariable String countryCode,
        @PathVariable String code
    ) {
        return toResponse(regionDirectory.regionByCountryAndCode(countryCode, code));
    }

    @GetMapping("/regions")
    public PageResult<InventoryRegionResponse> listRegions(
        @RequestParam(required = false) String countryCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return regionDirectory.listRegions(countryCode, PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryCountryResponse toResponse(InventoryCountryView country) {
        return new InventoryCountryResponse(
            country.id(),
            country.code(),
            country.name(),
            country.createdAt()
        );
    }

    private InventoryRegionResponse toResponse(InventoryRegionView region) {
        return new InventoryRegionResponse(
            region.id(),
            region.countryCode(),
            region.code(),
            region.name(),
            region.createdAt()
        );
    }
}
