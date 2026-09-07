package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryFacilityPartyRoleAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryFacilityPartyRoleAssignmentView;
import com.arcanaerp.platform.inventory.RegisterInventoryFacilityPartyRoleAssignmentCommand;
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
@RequestMapping("/api/inventory/facility-party-role-assignments")
@RequiredArgsConstructor
public class InventoryFacilityPartyRoleAssignmentController {

    private final InventoryFacilityPartyRoleAssignmentDirectory assignmentDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFacilityPartyRoleAssignmentResponse createAssignment(
        @Valid @RequestBody CreateInventoryFacilityPartyRoleAssignmentRequest request
    ) {
        Instant fromDate = parseOptionalInstant(request.fromDate(), "fromDate");
        Instant thruDate = parseOptionalInstant(request.thruDate(), "thruDate");
        validateDateRange(fromDate, thruDate);
        return toResponse(assignmentDirectory.registerAssignment(
            new RegisterInventoryFacilityPartyRoleAssignmentCommand(
                request.facilityCode(),
                request.partyCode(),
                request.roleTypeCode(),
                request.comments(),
                fromDate,
                thruDate,
                request.assignedBy()
            )
        ));
    }

    @GetMapping("/{id}")
    public InventoryFacilityPartyRoleAssignmentResponse assignmentById(@PathVariable UUID id) {
        return toResponse(assignmentDirectory.assignmentById(id));
    }

    @GetMapping
    public PageResult<InventoryFacilityPartyRoleAssignmentResponse> listAssignments(
        @RequestParam(required = false) String facilityCode,
        @RequestParam(required = false) String partyCode,
        @RequestParam(required = false) String roleTypeCode,
        @RequestParam(required = false) String assignedBy,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return assignmentDirectory.listAssignments(
            facilityCode,
            partyCode,
            roleTypeCode,
            assignedBy,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryFacilityPartyRoleAssignmentResponse toResponse(
        InventoryFacilityPartyRoleAssignmentView assignment
    ) {
        return new InventoryFacilityPartyRoleAssignmentResponse(
            assignment.id(),
            assignment.inventoryFacilityId(),
            assignment.facilityCode(),
            assignment.partyCode(),
            assignment.roleTypeCode(),
            assignment.comments(),
            assignment.fromDate(),
            assignment.thruDate(),
            assignment.assignedBy(),
            assignment.assignedAt()
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

    private static void validateDateRange(Instant fromDate, Instant thruDate) {
        if (fromDate != null && thruDate != null && fromDate.isAfter(thruDate)) {
            throw new IllegalArgumentException("fromDate must be before or equal to thruDate");
        }
    }
}
