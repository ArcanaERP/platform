package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipDirectory;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipView;
import com.arcanaerp.platform.inventory.RegisterInventoryEntryRelationshipCommand;
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
@RequestMapping("/api/inventory/entry-relationships")
@RequiredArgsConstructor
public class InventoryEntryRelationshipController {

    private final InventoryEntryRelationshipDirectory relationshipDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryEntryRelationshipResponse createRelationship(
        @Valid @RequestBody CreateInventoryEntryRelationshipRequest request
    ) {
        return toResponse(relationshipDirectory.registerRelationship(new RegisterInventoryEntryRelationshipCommand(
            request.relationshipTypeCode(),
            request.fromSku(),
            request.fromLocationCode(),
            request.toSku(),
            request.toLocationCode(),
            request.fromRoleTypeCode(),
            request.toRoleTypeCode(),
            request.description(),
            request.statusCode()
        )));
    }

    @GetMapping("/{id}")
    public InventoryEntryRelationshipResponse relationshipById(@PathVariable UUID id) {
        return toResponse(relationshipDirectory.relationshipById(id));
    }

    @GetMapping
    public PageResult<InventoryEntryRelationshipResponse> listRelationships(
        @RequestParam(required = false) String relationshipTypeCode,
        @RequestParam(required = false) String fromSku,
        @RequestParam(required = false) String fromLocationCode,
        @RequestParam(required = false) String toSku,
        @RequestParam(required = false) String toLocationCode,
        @RequestParam(required = false) String statusCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return relationshipDirectory.listRelationships(
            relationshipTypeCode,
            fromSku,
            fromLocationCode,
            toSku,
            toLocationCode,
            statusCode,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryEntryRelationshipResponse toResponse(InventoryEntryRelationshipView relationship) {
        return new InventoryEntryRelationshipResponse(
            relationship.id(),
            relationship.relationshipTypeCode(),
            relationship.fromSku(),
            relationship.fromLocationCode(),
            relationship.toSku(),
            relationship.toLocationCode(),
            relationship.fromRoleTypeCode(),
            relationship.toRoleTypeCode(),
            relationship.description(),
            relationship.statusCode(),
            relationship.createdAt()
        );
    }
}
