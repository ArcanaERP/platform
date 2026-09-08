package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.EndInventoryFixedAssetPartyRoleAssignmentCommand;
import com.arcanaerp.platform.inventory.InventoryFixedAssetPartyRoleAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetPartyRoleAssignmentEndView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetPartyRoleAssignmentView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetPartyRoleAssignmentCommand;
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
@RequestMapping("/api/inventory/fixed-asset-party-role-assignments")
@RequiredArgsConstructor
public class InventoryFixedAssetPartyRoleAssignmentController {

    private final InventoryFixedAssetPartyRoleAssignmentDirectory assignmentDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryFixedAssetPartyRoleAssignmentResponse createAssignment(
        @Valid @RequestBody CreateInventoryFixedAssetPartyRoleAssignmentRequest request
    ) {
        Instant fromDate = parseOptionalInstant(request.fromDate(), "fromDate");
        Instant thruDate = parseOptionalInstant(request.thruDate(), "thruDate");
        validateDateRange(fromDate, thruDate);
        return toResponse(assignmentDirectory.registerAssignment(
            new RegisterInventoryFixedAssetPartyRoleAssignmentCommand(
                request.fixedAssetCode(),
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
    public InventoryFixedAssetPartyRoleAssignmentResponse assignmentById(@PathVariable UUID id) {
        return toResponse(assignmentDirectory.assignmentById(id));
    }

    @PatchMapping("/{id}/end")
    public InventoryFixedAssetPartyRoleAssignmentResponse endAssignment(
        @PathVariable UUID id,
        @Valid @RequestBody EndInventoryFixedAssetPartyRoleAssignmentRequest request
    ) {
        Instant thruDate = parseRequiredInstant(request.thruDate(), "thruDate");
        return toResponse(assignmentDirectory.endAssignment(
            id,
            new EndInventoryFixedAssetPartyRoleAssignmentCommand(
                id,
                thruDate,
                request.reason(),
                request.endedBy()
            )
        ));
    }

    @GetMapping("/{id}/end-history")
    public PageResult<InventoryFixedAssetPartyRoleAssignmentEndResponse> listEndHistory(
        @PathVariable UUID id,
        @RequestParam(required = false) String endedBy,
        @RequestParam(required = false) String endedAtFrom,
        @RequestParam(required = false) String endedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedEndedAtFrom = parseOptionalInstant(endedAtFrom, "endedAtFrom");
        Instant parsedEndedAtTo = parseOptionalInstant(endedAtTo, "endedAtTo");
        validateDateRange(parsedEndedAtFrom, parsedEndedAtTo, "endedAtFrom", "endedAtTo");
        return assignmentDirectory.listEndHistory(
            id,
            endedBy,
            parsedEndedAtFrom,
            parsedEndedAtTo,
            PageQuery.of(page, size)
        ).map(this::toEndResponse);
    }

    @GetMapping
    public PageResult<InventoryFixedAssetPartyRoleAssignmentResponse> listAssignments(
        @RequestParam(required = false) String fixedAssetCode,
        @RequestParam(required = false) String partyCode,
        @RequestParam(required = false) String roleTypeCode,
        @RequestParam(required = false) String assignedBy,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return assignmentDirectory.listAssignments(
            fixedAssetCode,
            partyCode,
            roleTypeCode,
            assignedBy,
            active,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryFixedAssetPartyRoleAssignmentResponse toResponse(
        InventoryFixedAssetPartyRoleAssignmentView assignment
    ) {
        return new InventoryFixedAssetPartyRoleAssignmentResponse(
            assignment.id(),
            assignment.inventoryFixedAssetId(),
            assignment.fixedAssetCode(),
            assignment.partyCode(),
            assignment.roleTypeCode(),
            assignment.comments(),
            assignment.fromDate(),
            assignment.thruDate(),
            assignment.assignedBy(),
            assignment.assignedAt(),
            assignment.active(),
            assignment.endReason(),
            assignment.endedBy(),
            assignment.endedAt()
        );
    }

    private InventoryFixedAssetPartyRoleAssignmentEndResponse toEndResponse(
        InventoryFixedAssetPartyRoleAssignmentEndView end
    ) {
        return new InventoryFixedAssetPartyRoleAssignmentEndResponse(
            end.id(),
            end.assignmentId(),
            end.inventoryFixedAssetId(),
            end.fixedAssetCode(),
            end.partyCode(),
            end.roleTypeCode(),
            end.previousThruDate(),
            end.currentThruDate(),
            end.reason(),
            end.endedBy(),
            end.endedAt()
        );
    }

    private static Instant parseRequiredInstant(String value, String parameterName) {
        return parseOptionalInstant(value, parameterName);
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
        validateDateRange(fromDate, thruDate, "fromDate", "thruDate");
    }

    private static void validateDateRange(
        Instant fromDate,
        Instant thruDate,
        String fromParameterName,
        String thruParameterName
    ) {
        if (fromDate != null && thruDate != null && fromDate.isAfter(thruDate)) {
            throw new IllegalArgumentException(fromParameterName + " must be before or equal to " + thruParameterName);
        }
    }
}
