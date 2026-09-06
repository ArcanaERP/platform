package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipTypeView;
import com.arcanaerp.platform.inventory.InventoryEntryRoleTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryEntryRoleTypeView;
import com.arcanaerp.platform.inventory.RegisterInventoryEntryRelationshipTypeCommand;
import com.arcanaerp.platform.inventory.RegisterInventoryEntryRoleTypeCommand;
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
public class InventoryEntryReferenceDataController {

    private final InventoryEntryRelationshipTypeDirectory relationshipTypeDirectory;
    private final InventoryEntryRoleTypeDirectory roleTypeDirectory;

    @PostMapping("/entry-relationship-types")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryEntryRelationshipTypeResponse createRelationshipType(
        @Valid @RequestBody CreateInventoryEntryRelationshipTypeRequest request
    ) {
        return toResponse(relationshipTypeDirectory.registerRelationshipType(
            new RegisterInventoryEntryRelationshipTypeCommand(request.code(), request.description(), request.comments())
        ));
    }

    @GetMapping("/entry-relationship-types/{code}")
    public InventoryEntryRelationshipTypeResponse relationshipTypeByCode(@PathVariable String code) {
        return toResponse(relationshipTypeDirectory.relationshipTypeByCode(code));
    }

    @GetMapping("/entry-relationship-types")
    public PageResult<InventoryEntryRelationshipTypeResponse> listRelationshipTypes(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return relationshipTypeDirectory.listRelationshipTypes(PageQuery.of(page, size)).map(this::toResponse);
    }

    @PostMapping("/entry-role-types")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryEntryRoleTypeResponse createRoleType(
        @Valid @RequestBody CreateInventoryEntryRoleTypeRequest request
    ) {
        return toResponse(roleTypeDirectory.registerRoleType(
            new RegisterInventoryEntryRoleTypeCommand(request.code(), request.description(), request.comments())
        ));
    }

    @GetMapping("/entry-role-types/{code}")
    public InventoryEntryRoleTypeResponse roleTypeByCode(@PathVariable String code) {
        return toResponse(roleTypeDirectory.roleTypeByCode(code));
    }

    @GetMapping("/entry-role-types")
    public PageResult<InventoryEntryRoleTypeResponse> listRoleTypes(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return roleTypeDirectory.listRoleTypes(PageQuery.of(page, size)).map(this::toResponse);
    }

    private InventoryEntryRelationshipTypeResponse toResponse(InventoryEntryRelationshipTypeView type) {
        return new InventoryEntryRelationshipTypeResponse(
            type.id(),
            type.code(),
            type.description(),
            type.comments(),
            type.createdAt()
        );
    }

    private InventoryEntryRoleTypeResponse toResponse(InventoryEntryRoleTypeView type) {
        return new InventoryEntryRoleTypeResponse(
            type.id(),
            type.code(),
            type.description(),
            type.comments(),
            type.createdAt()
        );
    }
}
