package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetTypeCommand;
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
@RequestMapping("/api/inventory/fixed-asset-types")
@RequiredArgsConstructor
public class InventoryFixedAssetTypeController {

    private final InventoryFixedAssetTypeDirectory inventoryFixedAssetTypeDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFixedAssetTypeResponse createFixedAssetType(
        @Valid @RequestBody CreateInventoryFixedAssetTypeRequest request
    ) {
        return toResponse(inventoryFixedAssetTypeDirectory.registerFixedAssetType(
            new RegisterInventoryFixedAssetTypeCommand(request.code(), request.description())
        ));
    }

    @GetMapping("/{code}")
    public InventoryFixedAssetTypeResponse fixedAssetTypeByCode(@PathVariable String code) {
        return toResponse(inventoryFixedAssetTypeDirectory.fixedAssetTypeByCode(code));
    }

    @GetMapping
    public PageResult<InventoryFixedAssetTypeResponse> listFixedAssetTypes(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryFixedAssetTypeDirectory.listFixedAssetTypes(PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryFixedAssetTypeResponse toResponse(InventoryFixedAssetTypeView type) {
        return new InventoryFixedAssetTypeResponse(
            type.id(),
            type.code(),
            type.description(),
            type.createdAt()
        );
    }
}
