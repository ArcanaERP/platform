package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryContactPurposeDirectory;
import com.arcanaerp.platform.inventory.InventoryTelecomContactDirectory;
import com.arcanaerp.platform.inventory.InventoryTelecomContactMetadataChangeView;
import com.arcanaerp.platform.inventory.InventoryTelecomContactView;
import com.arcanaerp.platform.inventory.RegisterInventoryTelecomContactCommand;
import com.arcanaerp.platform.inventory.UpdateInventoryTelecomContactMetadataCommand;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class InventoryTelecomContactDirectoryService implements InventoryTelecomContactDirectory {

    private static final String OWNER_TYPE_LOCATION = "LOCATION";
    private static final String OWNER_TYPE_FACILITY = "FACILITY";

    private final InventoryTelecomContactRepository inventoryTelecomContactRepository;
    private final InventoryTelecomContactMetadataChangeAuditRepository metadataChangeAuditRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final InventoryFacilityRepository inventoryFacilityRepository;
    private final InventoryContactPurposeDirectory inventoryContactPurposeDirectory;
    private final Clock clock;

    @Override
    public InventoryTelecomContactView registerTelecomContact(RegisterInventoryTelecomContactCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String ownerType = normalizeOwnerType(command.ownerType());
        String ownerCode = normalizeRequired(command.ownerCode(), "ownerCode").toUpperCase();
        String contactPurposeCode = normalizeRequired(command.contactPurposeCode(), "contactPurposeCode").toUpperCase();
        String telecomType = InventoryTelecomContact.normalizeTelecomType(command.telecomType());
        ensureOwnerExists(ownerType, ownerCode);
        ensureContactPurposeExists(contactPurposeCode);
        if (
            inventoryTelecomContactRepository
                .findByOwnerTypeAndOwnerCodeAndContactPurposeCodeAndTelecomType(
                    ownerType,
                    ownerCode,
                    contactPurposeCode,
                    telecomType
                )
                .isPresent()
        ) {
            throw new ConflictException(
                "Inventory telecom contact already exists for owner: "
                    + ownerType
                    + "/"
                    + ownerCode
                    + ", purpose: "
                    + contactPurposeCode
                    + ", and type: "
                    + telecomType
            );
        }
        return toView(inventoryTelecomContactRepository.save(InventoryTelecomContact.create(
            ownerType,
            ownerCode,
            contactPurposeCode,
            telecomType,
            command.contactName(),
            command.contactValue(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryTelecomContactView telecomContactById(UUID id) {
        return toView(findTelecomContact(id));
    }

    @Override
    public InventoryTelecomContactView updateTelecomContactMetadata(
        UUID id,
        UpdateInventoryTelecomContactMetadataCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryTelecomContact contact = findTelecomContact(id);
        String contactPurposeCode = normalizeRequired(command.contactPurposeCode(), "contactPurposeCode").toUpperCase();
        String telecomType = InventoryTelecomContact.normalizeTelecomType(command.telecomType());
        ensureContactPurposeExists(contactPurposeCode);
        if (
            (!contact.getContactPurposeCode().equals(contactPurposeCode) || !contact.getTelecomType().equals(telecomType))
                && inventoryTelecomContactRepository
                    .findByOwnerTypeAndOwnerCodeAndContactPurposeCodeAndTelecomType(
                        contact.getOwnerType(),
                        contact.getOwnerCode(),
                        contactPurposeCode,
                        telecomType
                    )
                    .isPresent()
        ) {
            throw new ConflictException(
                "Inventory telecom contact already exists for owner: "
                    + contact.getOwnerType()
                    + "/"
                    + contact.getOwnerCode()
                    + ", purpose: "
                    + contactPurposeCode
                    + ", and type: "
                    + telecomType
            );
        }
        InventoryTelecomContactMetadataSnapshot previous = InventoryTelecomContactMetadataSnapshot.from(contact);
        Instant changedAt = Instant.now(clock);
        contact.updateMetadata(
            command.contactPurposeCode(),
            command.telecomType(),
            command.contactName(),
            command.contactValue(),
            changedAt
        );
        metadataChangeAuditRepository.save(InventoryTelecomContactMetadataChangeAudit.create(
            contact.getId(),
            contact.getOwnerType(),
            contact.getOwnerCode(),
            previous,
            InventoryTelecomContactMetadataSnapshot.from(contact),
            command.changedBy(),
            changedAt
        ));
        return toView(inventoryTelecomContactRepository.save(contact));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryTelecomContactMetadataChangeView> listMetadataHistory(
        UUID id,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        InventoryTelecomContact contact = findTelecomContact(id);
        Page<InventoryTelecomContactMetadataChangeAudit> history = metadataChangeAuditRepository.findHistoryFiltered(
            contact.getId(),
            normalizeOptionalChangedBy(changedBy),
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(history).map(this::toMetadataChangeView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryTelecomContactView> listTelecomContacts(
        String ownerType,
        String ownerCode,
        String contactPurposeCode,
        String telecomType,
        PageQuery pageQuery
    ) {
        return PageResult.from(inventoryTelecomContactRepository.findFiltered(
            normalizeOptionalOwnerType(ownerType),
            normalizeOptionalUpper(ownerCode, "ownerCode"),
            normalizeOptionalUpper(contactPurposeCode, "contactPurposeCode"),
            normalizeOptionalTelecomType(telecomType),
            pageQuery.toPageable(
                Sort.by(Sort.Direction.ASC, "ownerType")
                    .and(Sort.by("ownerCode"))
                    .and(Sort.by("contactPurposeCode"))
                    .and(Sort.by("telecomType"))
            )
        )).map(this::toView);
    }

    private InventoryTelecomContact findTelecomContact(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return inventoryTelecomContactRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Inventory telecom contact not found for id: " + id));
    }

    private void ensureOwnerExists(String ownerType, String ownerCode) {
        if (OWNER_TYPE_LOCATION.equals(ownerType)) {
            if (inventoryLocationRepository.findByCode(ownerCode).isEmpty()) {
                throw new IllegalArgumentException("Inventory location not found: " + ownerCode);
            }
            return;
        }
        if (OWNER_TYPE_FACILITY.equals(ownerType)) {
            if (inventoryFacilityRepository.findByCode(ownerCode).isEmpty()) {
                throw new IllegalArgumentException("Inventory facility not found: " + ownerCode);
            }
            return;
        }
        throw new IllegalArgumentException("ownerType must be LOCATION or FACILITY");
    }

    private void ensureContactPurposeExists(String contactPurposeCode) {
        if (!inventoryContactPurposeDirectory.contactPurposeExists(contactPurposeCode)) {
            throw new IllegalArgumentException("Inventory contact purpose not found: " + contactPurposeCode);
        }
    }

    private InventoryTelecomContactView toView(InventoryTelecomContact contact) {
        return new InventoryTelecomContactView(
            contact.getId(),
            contact.getOwnerType(),
            contact.getOwnerCode(),
            contact.getContactPurposeCode(),
            contact.getTelecomType(),
            contact.getContactName(),
            contact.getContactValue(),
            contact.getCreatedAt(),
            contact.getUpdatedAt()
        );
    }

    private InventoryTelecomContactMetadataChangeView toMetadataChangeView(
        InventoryTelecomContactMetadataChangeAudit audit
    ) {
        return new InventoryTelecomContactMetadataChangeView(
            audit.getId(),
            audit.getTelecomContactId(),
            audit.getOwnerType(),
            audit.getOwnerCode(),
            audit.getPreviousContactPurposeCode(),
            audit.getCurrentContactPurposeCode(),
            audit.getPreviousTelecomType(),
            audit.getCurrentTelecomType(),
            audit.getPreviousContactName(),
            audit.getCurrentContactName(),
            audit.getPreviousContactValue(),
            audit.getCurrentContactValue(),
            audit.getChangedBy(),
            audit.getChangedAt()
        );
    }

    private static String normalizeOwnerType(String value) {
        String normalized = normalizeRequired(value, "ownerType").toUpperCase();
        if (!OWNER_TYPE_LOCATION.equals(normalized) && !OWNER_TYPE_FACILITY.equals(normalized)) {
            throw new IllegalArgumentException("ownerType must be LOCATION or FACILITY");
        }
        return normalized;
    }

    private static String normalizeOptionalOwnerType(String value) {
        return value == null ? null : normalizeOwnerType(value);
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalUpper(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim().toUpperCase();
    }

    private static String normalizeOptionalChangedBy(String value) {
        return value == null ? null : normalizeRequired(value, "changedBy").toLowerCase();
    }

    private static String normalizeOptionalTelecomType(String value) {
        return value == null ? null : InventoryTelecomContact.normalizeTelecomType(value);
    }
}
