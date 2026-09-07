package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetCommand;
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
@RequestMapping("/api/inventory/fixed-assets")
@RequiredArgsConstructor
public class InventoryFixedAssetController {

    private final InventoryFixedAssetDirectory inventoryFixedAssetDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFixedAssetResponse createFixedAsset(@Valid @RequestBody CreateInventoryFixedAssetRequest request) {
        return toResponse(inventoryFixedAssetDirectory.registerFixedAsset(
            new RegisterInventoryFixedAssetCommand(
                request.code(),
                request.description(),
                request.fixedAssetTypeCode(),
                request.comments(),
                request.externalIdentifier(),
                request.externalIdSource()
            )
        ));
    }

    @GetMapping("/{code}")
    public InventoryFixedAssetResponse fixedAssetByCode(@PathVariable String code) {
        return toResponse(inventoryFixedAssetDirectory.fixedAssetByCode(code));
    }

    @GetMapping
    public PageResult<InventoryFixedAssetResponse> listFixedAssets(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryFixedAssetDirectory.listFixedAssets(active, query, PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryFixedAssetResponse toResponse(InventoryFixedAssetView fixedAsset) {
        return new InventoryFixedAssetResponse(
            fixedAsset.id(),
            fixedAsset.code(),
            fixedAsset.description(),
            fixedAsset.fixedAssetTypeCode(),
            fixedAsset.comments(),
            fixedAsset.externalIdentifier(),
            fixedAsset.externalIdSource(),
            fixedAsset.active(),
            fixedAsset.createdAt(),
            fixedAsset.updatedAt()
        );
    }
}
