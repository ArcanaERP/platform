package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryLocationTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryLocationTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryLocationTypeCommand;
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
            type.createdAt()
        );
    }
}
