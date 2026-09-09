package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetFacilityAssignmentTypeCommand;
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
@RequestMapping("/api/inventory/fixed-asset-facility-assignment-types")
@RequiredArgsConstructor
public class InventoryFixedAssetFacilityAssignmentTypeController {

    private final InventoryFixedAssetFacilityAssignmentTypeDirectory assignmentTypeDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFixedAssetFacilityAssignmentTypeResponse createAssignmentType(
        @Valid @RequestBody CreateInventoryFixedAssetFacilityAssignmentTypeRequest request
    ) {
        return toResponse(assignmentTypeDirectory.registerAssignmentType(
            new RegisterInventoryFixedAssetFacilityAssignmentTypeCommand(request.code(), request.description())
        ));
    }

    @GetMapping("/{code}")
    public InventoryFixedAssetFacilityAssignmentTypeResponse assignmentTypeByCode(@PathVariable String code) {
        return toResponse(assignmentTypeDirectory.assignmentTypeByCode(code));
    }

    @GetMapping
    public PageResult<InventoryFixedAssetFacilityAssignmentTypeResponse> listAssignmentTypes(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return assignmentTypeDirectory.listAssignmentTypes(PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryFixedAssetFacilityAssignmentTypeResponse toResponse(
        InventoryFixedAssetFacilityAssignmentTypeView type
    ) {
        return new InventoryFixedAssetFacilityAssignmentTypeResponse(
            type.id(),
            type.code(),
            type.description(),
            type.createdAt()
        );
    }
}
