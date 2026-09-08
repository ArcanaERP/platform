package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryContactPurposeDirectory;
import com.arcanaerp.platform.inventory.InventoryContactPurposeView;
import com.arcanaerp.platform.inventory.RegisterInventoryContactPurposeCommand;
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
@RequestMapping("/api/inventory/contact-purposes")
@RequiredArgsConstructor
public class InventoryContactPurposeController {

    private final InventoryContactPurposeDirectory inventoryContactPurposeDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryContactPurposeResponse createContactPurpose(
        @Valid @RequestBody CreateInventoryContactPurposeRequest request
    ) {
        return toResponse(inventoryContactPurposeDirectory.registerContactPurpose(
            new RegisterInventoryContactPurposeCommand(request.code(), request.description())
        ));
    }

    @GetMapping("/{code}")
    public InventoryContactPurposeResponse contactPurposeByCode(@PathVariable String code) {
        return toResponse(inventoryContactPurposeDirectory.contactPurposeByCode(code));
    }

    @GetMapping
    public PageResult<InventoryContactPurposeResponse> listContactPurposes(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryContactPurposeDirectory.listContactPurposes(PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryContactPurposeResponse toResponse(InventoryContactPurposeView purpose) {
        return new InventoryContactPurposeResponse(
            purpose.id(),
            purpose.code(),
            purpose.description(),
            purpose.createdAt()
        );
    }
}
