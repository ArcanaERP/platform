package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryProductInstanceAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryProductInstanceAssignmentView;
import com.arcanaerp.platform.inventory.RegisterInventoryProductInstanceAssignmentCommand;
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
@RequestMapping("/api/inventory/product-instance-assignments")
@RequiredArgsConstructor
public class InventoryProductInstanceAssignmentController {

    private final InventoryProductInstanceAssignmentDirectory assignmentDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryProductInstanceAssignmentResponse createAssignment(
        @Valid @RequestBody CreateInventoryProductInstanceAssignmentRequest request
    ) {
        return toResponse(assignmentDirectory.registerAssignment(new RegisterInventoryProductInstanceAssignmentCommand(
            request.sku(),
            request.locationCode(),
            request.productInstanceCode(),
            request.assignedBy()
        )));
    }

    @GetMapping("/{id}")
    public InventoryProductInstanceAssignmentResponse assignmentById(@PathVariable UUID id) {
        return toResponse(assignmentDirectory.assignmentById(id));
    }

    @GetMapping
    public PageResult<InventoryProductInstanceAssignmentResponse> listAssignments(
        @RequestParam(required = false) String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String productInstanceCode,
        @RequestParam(required = false) String assignedBy,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return assignmentDirectory.listAssignments(
            sku,
            locationCode,
            productInstanceCode,
            assignedBy,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryProductInstanceAssignmentResponse toResponse(InventoryProductInstanceAssignmentView assignment) {
        return new InventoryProductInstanceAssignmentResponse(
            assignment.id(),
            assignment.inventoryItemId(),
            assignment.sku(),
            assignment.locationCode(),
            assignment.productInstanceCode(),
            assignment.assignedBy(),
            assignment.assignedAt()
        );
    }
}
