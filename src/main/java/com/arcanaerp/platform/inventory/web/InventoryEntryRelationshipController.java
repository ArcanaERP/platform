package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipDirectory;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipStatusChangeView;
import com.arcanaerp.platform.inventory.InventoryEntryRelationshipView;
import com.arcanaerp.platform.inventory.RegisterInventoryEntryRelationshipCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryEntryRelationshipStatusCommand;
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

    @PatchMapping("/{id}/status")
    public InventoryEntryRelationshipResponse updateRelationshipStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInventoryEntryRelationshipStatusRequest request
    ) {
        return toResponse(relationshipDirectory.updateRelationshipStatus(
            id,
            new UpdateInventoryEntryRelationshipStatusCommand(
                id,
                request.statusCode(),
                request.reason(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{id}/status-history")
    public PageResult<InventoryEntryRelationshipStatusChangeResponse> listStatusHistory(
        @PathVariable UUID id,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return relationshipDirectory.listStatusHistory(
            id,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toStatusChangeResponse);
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
            relationship.createdAt(),
            relationship.updatedAt()
        );
    }

    private InventoryEntryRelationshipStatusChangeResponse toStatusChangeResponse(
        InventoryEntryRelationshipStatusChangeView change
    ) {
        return new InventoryEntryRelationshipStatusChangeResponse(
            change.id(),
            change.relationshipId(),
            change.previousStatusCode(),
            change.currentStatusCode(),
            change.reason(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private static String normalizeOptionalChangedBy(String changedBy) {
        if (changedBy == null) {
            return null;
        }
        if (changedBy.isBlank()) {
            throw new IllegalArgumentException("changedBy query parameter must not be blank");
        }
        return changedBy.trim().toLowerCase();
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

    private static void validateChangedAtRange(Instant changedAtFrom, Instant changedAtTo) {
        if (changedAtFrom != null && changedAtTo != null && changedAtFrom.isAfter(changedAtTo)) {
            throw new IllegalArgumentException("changedAtFrom must be before or equal to changedAtTo");
        }
    }
}
