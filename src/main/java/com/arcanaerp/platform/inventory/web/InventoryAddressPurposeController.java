package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryAddressPurposeDirectory;
import com.arcanaerp.platform.inventory.InventoryAddressPurposeView;
import com.arcanaerp.platform.inventory.RegisterInventoryAddressPurposeCommand;
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
@RequestMapping("/api/inventory/address-purposes")
@RequiredArgsConstructor
public class InventoryAddressPurposeController {

    private final InventoryAddressPurposeDirectory inventoryAddressPurposeDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryAddressPurposeResponse createAddressPurpose(
        @Valid @RequestBody CreateInventoryAddressPurposeRequest request
    ) {
        return toResponse(inventoryAddressPurposeDirectory.registerAddressPurpose(
            new RegisterInventoryAddressPurposeCommand(request.code(), request.description())
        ));
    }

    @GetMapping("/{code}")
    public InventoryAddressPurposeResponse addressPurposeByCode(@PathVariable String code) {
        return toResponse(inventoryAddressPurposeDirectory.addressPurposeByCode(code));
    }

    @GetMapping
    public PageResult<InventoryAddressPurposeResponse> listAddressPurposes(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryAddressPurposeDirectory.listAddressPurposes(PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryAddressPurposeResponse toResponse(InventoryAddressPurposeView purpose) {
        return new InventoryAddressPurposeResponse(
            purpose.id(),
            purpose.code(),
            purpose.description(),
            purpose.createdAt()
        );
    }
}
