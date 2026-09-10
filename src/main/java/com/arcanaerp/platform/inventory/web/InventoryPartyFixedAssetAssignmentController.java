package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryPartyFixedAssetAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryPartyFixedAssetAssignmentView;
import com.arcanaerp.platform.inventory.RegisterInventoryPartyFixedAssetAssignmentCommand;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
@RequestMapping("/api/inventory/party-fixed-asset-assignments")
@RequiredArgsConstructor
public class InventoryPartyFixedAssetAssignmentController {

    private final InventoryPartyFixedAssetAssignmentDirectory assignmentDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryPartyFixedAssetAssignmentResponse createAssignment(
        @Valid @RequestBody CreateInventoryPartyFixedAssetAssignmentRequest request
    ) {
        Instant assignedFrom = parseOptionalInstant(request.assignedFrom(), "assignedFrom");
        Instant assignedThru = parseOptionalInstant(request.assignedThru(), "assignedThru");
        validateDateRange(assignedFrom, assignedThru);
        return toResponse(assignmentDirectory.registerAssignment(new RegisterInventoryPartyFixedAssetAssignmentCommand(
            request.partyCode(),
            request.fixedAssetCode(),
            assignedFrom,
            assignedThru,
            request.allocatedCostMoneyId()
        )));
    }

    @GetMapping("/{id}")
    public InventoryPartyFixedAssetAssignmentResponse assignmentById(@PathVariable UUID id) {
        return toResponse(assignmentDirectory.assignmentById(id));
    }

    @GetMapping
    public PageResult<InventoryPartyFixedAssetAssignmentResponse> listAssignments(
        @RequestParam(required = false) String partyCode,
        @RequestParam(required = false) String fixedAssetCode,
        @RequestParam(required = false) Long allocatedCostMoneyId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return assignmentDirectory.listAssignments(
            partyCode,
            fixedAssetCode,
            allocatedCostMoneyId,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryPartyFixedAssetAssignmentResponse toResponse(InventoryPartyFixedAssetAssignmentView assignment) {
        return new InventoryPartyFixedAssetAssignmentResponse(
            assignment.id(),
            assignment.inventoryPartyId(),
            assignment.partyCode(),
            assignment.inventoryFixedAssetId(),
            assignment.fixedAssetCode(),
            assignment.assignedFrom(),
            assignment.assignedThru(),
            assignment.allocatedCostMoneyId(),
            assignment.createdAt()
        );
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " must be a valid ISO-8601 instant");
        }
    }

    private static void validateDateRange(Instant assignedFrom, Instant assignedThru) {
        if (assignedFrom != null && assignedThru != null && assignedFrom.isAfter(assignedThru)) {
            throw new IllegalArgumentException("assignedFrom must be before or equal to assignedThru");
        }
    }
}
