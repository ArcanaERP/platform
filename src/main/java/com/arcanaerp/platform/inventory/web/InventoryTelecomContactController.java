package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryTelecomContactDirectory;
import com.arcanaerp.platform.inventory.InventoryTelecomContactMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryTelecomContactView;
import com.arcanaerp.platform.inventory.RegisterInventoryTelecomContactCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryTelecomContactMetadataCommand;
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
@RequestMapping("/api/inventory/telecom-contacts")
@RequiredArgsConstructor
public class InventoryTelecomContactController {

    private final InventoryTelecomContactDirectory inventoryTelecomContactDirectory;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryTelecomContactResponse createTelecomContact(
        @Valid @RequestBody CreateInventoryTelecomContactRequest request
    ) {
        return toResponse(inventoryTelecomContactDirectory.registerTelecomContact(
            new RegisterInventoryTelecomContactCommand(
                request.ownerType(),
                request.ownerCode(),
                request.contactPurposeCode(),
                request.telecomType(),
                request.contactName(),
                request.contactValue()
            )
        ));
    }

    @GetMapping("/{id}")
    public InventoryTelecomContactResponse telecomContactById(@PathVariable UUID id) {
        return toResponse(inventoryTelecomContactDirectory.telecomContactById(id));
    }

    @PatchMapping("/{id}/metadata")
    public InventoryTelecomContactResponse updateTelecomContactMetadata(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInventoryTelecomContactMetadataRequest request
    ) {
        return toResponse(inventoryTelecomContactDirectory.updateTelecomContactMetadata(
            id,
            new UpdateInventoryTelecomContactMetadataCommand(
                request.contactPurposeCode(),
                request.telecomType(),
                request.contactName(),
                request.contactValue(),
                request.changedBy()
            )
        ));
    }

    @GetMapping("/{id}/metadata-history")
    public PageResult<InventoryTelecomContactMetadataChangeResponse> listMetadataHistory(
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
        return inventoryTelecomContactDirectory.listMetadataHistory(
            id,
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMetadataChangeResponse);
    }

    @GetMapping
    public PageResult<InventoryTelecomContactResponse> listTelecomContacts(
        @RequestParam(required = false) String ownerType,
        @RequestParam(required = false) String ownerCode,
        @RequestParam(required = false) String contactPurposeCode,
        @RequestParam(required = false) String telecomType,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryTelecomContactDirectory.listTelecomContacts(
            ownerType,
            ownerCode,
            contactPurposeCode,
            telecomType,
            PageQuery.of(page, size)
        ).map(this::toResponse);
    }

    private InventoryTelecomContactResponse toResponse(InventoryTelecomContactView contact) {
        return new InventoryTelecomContactResponse(
            contact.id(),
            contact.ownerType(),
            contact.ownerCode(),
            contact.contactPurposeCode(),
            contact.telecomType(),
            contact.contactName(),
            contact.contactValue(),
            contact.createdAt(),
            contact.updatedAt()
        );
    }

    private InventoryTelecomContactMetadataChangeResponse toMetadataChangeResponse(
        InventoryTelecomContactMetadataChangeView change
    ) {
        return new InventoryTelecomContactMetadataChangeResponse(
            change.id(),
            change.telecomContactId(),
            change.ownerType(),
            change.ownerCode(),
            change.previousContactPurposeCode(),
            change.currentContactPurposeCode(),
            change.previousTelecomType(),
            change.currentTelecomType(),
            change.previousContactName(),
            change.currentContactName(),
            change.previousContactValue(),
            change.currentContactValue(),
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
