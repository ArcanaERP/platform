package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryProductInstanceAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryProductInstanceAssignmentReleaseView;
import com.arcanaerp.platform.inventory.InventoryProductInstanceAssignmentView;
import com.arcanaerp.platform.inventory.ReleaseInventoryProductInstanceAssignmentCommand;
import com.arcanaerp.platform.inventory.RegisterInventoryProductInstanceAssignmentCommand;
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

    @PatchMapping("/{id}/release")
    public InventoryProductInstanceAssignmentResponse releaseAssignment(
        @PathVariable UUID id,
        @Valid @RequestBody ReleaseInventoryProductInstanceAssignmentRequest request
    ) {
        return toResponse(assignmentDirectory.releaseAssignment(
            id,
            new ReleaseInventoryProductInstanceAssignmentCommand(
                id,
                request.reason(),
                request.releasedBy()
            )
        ));
    }

    @GetMapping("/{id}/release-history")
    public PageResult<InventoryProductInstanceAssignmentReleaseResponse> listReleaseHistory(
        @PathVariable UUID id,
        @RequestParam(required = false) String releasedBy,
        @RequestParam(required = false) String releasedAtFrom,
        @RequestParam(required = false) String releasedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedReleasedAtFrom = parseOptionalInstant(releasedAtFrom, "releasedAtFrom");
        Instant parsedReleasedAtTo = parseOptionalInstant(releasedAtTo, "releasedAtTo");
        validateReleasedAtRange(parsedReleasedAtFrom, parsedReleasedAtTo);
        return assignmentDirectory.listReleaseHistory(
            id,
            normalizeOptionalReleasedBy(releasedBy),
            parsedReleasedAtFrom,
            parsedReleasedAtTo,
            PageQuery.of(page, size)
        ).map(this::toReleaseResponse);
    }

    @GetMapping
    public PageResult<InventoryProductInstanceAssignmentResponse> listAssignments(
        @RequestParam(required = false) String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String productInstanceCode,
        @RequestParam(required = false) String assignedBy,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return assignmentDirectory.listAssignments(
            sku,
            locationCode,
            productInstanceCode,
            assignedBy,
            active,
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
            assignment.assignedAt(),
            assignment.active(),
            assignment.releaseReason(),
            assignment.releasedBy(),
            assignment.releasedAt()
        );
    }

    private InventoryProductInstanceAssignmentReleaseResponse toReleaseResponse(
        InventoryProductInstanceAssignmentReleaseView release
    ) {
        return new InventoryProductInstanceAssignmentReleaseResponse(
            release.id(),
            release.assignmentId(),
            release.inventoryItemId(),
            release.sku(),
            release.locationCode(),
            release.productInstanceCode(),
            release.reason(),
            release.releasedBy(),
            release.releasedAt()
        );
    }

    private static String normalizeOptionalReleasedBy(String releasedBy) {
        if (releasedBy == null) {
            return null;
        }
        if (releasedBy.isBlank()) {
            throw new IllegalArgumentException("releasedBy query parameter must not be blank");
        }
        return releasedBy.trim().toLowerCase();
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be a valid ISO-8601 instant");
        }
    }

    private static void validateReleasedAtRange(Instant releasedAtFrom, Instant releasedAtTo) {
        if (releasedAtFrom != null && releasedAtTo != null && releasedAtFrom.isAfter(releasedAtTo)) {
            throw new IllegalArgumentException("releasedAtFrom must be before or equal to releasedAtTo");
        }
    }
}
