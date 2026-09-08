package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.EndInventoryItemLocationAssignmentCommand;
import com.arcanaerp.platform.inventory.InventoryItemLocationAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryItemLocationAssignmentEndView;
import com.arcanaerp.platform.inventory.InventoryItemLocationAssignmentView;
import com.arcanaerp.platform.inventory.RegisterInventoryItemLocationAssignmentCommand;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/item-location-assignments")
@RequiredArgsConstructor
public class InventoryItemLocationAssignmentController {

    private final InventoryItemLocationAssignmentDirectory assignmentDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemLocationAssignmentResponse createAssignment(
        @Valid @RequestBody CreateInventoryItemLocationAssignmentRequest request
    ) {
        return toResponse(assignmentDirectory.registerAssignment(new RegisterInventoryItemLocationAssignmentCommand(
            request.sku(),
            request.itemLocationCode(),
            request.assignedLocationCode(),
            request.assignedFacilityCode(),
            request.assignedStorageAreaCode(),
            parseOptionalInstant(request.validFrom(), "validFrom"),
            request.assignedBy()
        )));
    }

    @GetMapping("/{id}")
    public InventoryItemLocationAssignmentResponse assignmentById(@PathVariable UUID id) {
        return toResponse(assignmentDirectory.assignmentById(id));
    }

    @PatchMapping("/{id}/end")
    public InventoryItemLocationAssignmentResponse endAssignment(
        @PathVariable UUID id,
        @Valid @RequestBody EndInventoryItemLocationAssignmentRequest request
    ) {
        return toResponse(assignmentDirectory.endAssignment(
            id,
            new EndInventoryItemLocationAssignmentCommand(
                id,
                parseRequiredInstant(request.validThru(), "validThru"),
                request.reason(),
                request.endedBy()
            )
        ));
    }

    @GetMapping("/{id}/end-history")
    public PageResult<InventoryItemLocationAssignmentEndResponse> listEndHistory(
        @PathVariable UUID id,
        @RequestParam(required = false) String endedBy,
        @RequestParam(required = false) String endedAtFrom,
        @RequestParam(required = false) String endedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedEndedAtFrom = parseOptionalInstant(endedAtFrom, "endedAtFrom");
        Instant parsedEndedAtTo = parseOptionalInstant(endedAtTo, "endedAtTo");
        validateInstantRange(parsedEndedAtFrom, parsedEndedAtTo, "endedAtFrom", "endedAtTo");
        return assignmentDirectory.listEndHistory(
            id,
            normalizeOptionalEndedBy(endedBy),
            parsedEndedAtFrom,
            parsedEndedAtTo,
            PageQuery.of(page, size)
        ).map(this::toEndResponse);
    }

    @GetMapping
    public PageResult<InventoryItemLocationAssignmentResponse> listAssignments(
        @RequestParam(required = false) String sku,
        @RequestParam(required = false) String itemLocationCode,
        @RequestParam(required = false) String assignedLocationCode,
        @RequestParam(required = false) String assignedFacilityCode,
        @RequestParam(required = false) String assignedStorageAreaCode,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return assignmentDirectory.listAssignments(
            sku,
            itemLocationCode,
            assignedLocationCode,
            assignedFacilityCode,
            assignedStorageAreaCode,
            active,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryItemLocationAssignmentResponse toResponse(InventoryItemLocationAssignmentView assignment) {
        return new InventoryItemLocationAssignmentResponse(
            assignment.id(),
            assignment.inventoryItemId(),
            assignment.sku(),
            assignment.itemLocationCode(),
            assignment.assignedLocationCode(),
            assignment.assignedFacilityCode(),
            assignment.assignedStorageAreaCode(),
            assignment.validFrom(),
            assignment.validThru(),
            assignment.active(),
            assignment.assignedBy(),
            assignment.assignedAt(),
            assignment.endReason(),
            assignment.endedBy(),
            assignment.endedAt()
        );
    }

    private InventoryItemLocationAssignmentEndResponse toEndResponse(InventoryItemLocationAssignmentEndView end) {
        return new InventoryItemLocationAssignmentEndResponse(
            end.id(),
            end.assignmentId(),
            end.inventoryItemId(),
            end.sku(),
            end.itemLocationCode(),
            end.assignedLocationCode(),
            end.assignedFacilityCode(),
            end.assignedStorageAreaCode(),
            end.previousValidThru(),
            end.currentValidThru(),
            end.reason(),
            end.endedBy(),
            end.endedAt()
        );
    }

    private static String normalizeOptionalEndedBy(String endedBy) {
        if (endedBy == null) {
            return null;
        }
        if (endedBy.isBlank()) {
            throw new IllegalArgumentException("endedBy query parameter must not be blank");
        }
        return endedBy.trim().toLowerCase();
    }

    private static Instant parseRequiredInstant(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " is required");
        }
        return parseInstant(value, parameterName);
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " must not be blank");
        }
        return parseInstant(value, parameterName);
    }

    private static Instant parseInstant(String value, String parameterName) {
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " must be a valid ISO-8601 instant");
        }
    }

    private static void validateInstantRange(
        Instant start,
        Instant end,
        String startParameter,
        String endParameter
    ) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException(startParameter + " must be before or equal to " + endParameter);
        }
    }
}
