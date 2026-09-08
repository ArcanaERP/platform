package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryStorageAreaDirectory;
import com.arcanaerp.platform.inventory.InventoryStorageAreaView;
import com.arcanaerp.platform.inventory.RegisterInventoryStorageAreaCommand;
import jakarta.validation.Valid;
import java.util.UUID;
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
@RequestMapping("/api/inventory/storage-areas")
@RequiredArgsConstructor
public class InventoryStorageAreaController {

    private final InventoryStorageAreaDirectory inventoryStorageAreaDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryStorageAreaResponse createStorageArea(
        @Valid @RequestBody CreateInventoryStorageAreaRequest request
    ) {
        return toResponse(inventoryStorageAreaDirectory.registerStorageArea(new RegisterInventoryStorageAreaCommand(
            request.facilityCode(),
            request.code(),
            request.name(),
            request.storageAreaType(),
            request.parentStorageAreaCode()
        )));
    }

    @GetMapping("/{id}")
    public InventoryStorageAreaResponse storageAreaById(@PathVariable UUID id) {
        return toResponse(inventoryStorageAreaDirectory.storageAreaById(id));
    }

    @GetMapping
    public PageResult<InventoryStorageAreaResponse> listStorageAreas(
        @RequestParam(required = false) String facilityCode,
        @RequestParam(required = false) String storageAreaType,
        @RequestParam(required = false) String parentStorageAreaCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryStorageAreaDirectory.listStorageAreas(
            facilityCode,
            storageAreaType,
            parentStorageAreaCode,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryStorageAreaResponse toResponse(InventoryStorageAreaView storageArea) {
        return new InventoryStorageAreaResponse(
            storageArea.id(),
            storageArea.facilityCode(),
            storageArea.code(),
            storageArea.name(),
            storageArea.storageAreaType(),
            storageArea.parentStorageAreaCode(),
            storageArea.createdAt(),
            storageArea.updatedAt()
        );
    }
}
