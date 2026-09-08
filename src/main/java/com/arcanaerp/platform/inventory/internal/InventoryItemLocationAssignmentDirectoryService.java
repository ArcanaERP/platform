package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.EndInventoryItemLocationAssignmentCommand;
import com.arcanaerp.platform.inventory.InventoryItemLocationAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryItemLocationAssignmentEndView;
import com.arcanaerp.platform.inventory.InventoryItemLocationAssignmentView;
import com.arcanaerp.platform.inventory.RegisterInventoryItemLocationAssignmentCommand;
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
class InventoryItemLocationAssignmentDirectoryService implements InventoryItemLocationAssignmentDirectory {

    private final InventoryItemLocationAssignmentRepository assignmentRepository;
    private final InventoryItemLocationAssignmentEndAuditRepository endAuditRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final InventoryFacilityRepository inventoryFacilityRepository;
    private final InventoryStorageAreaRepository inventoryStorageAreaRepository;
    private final Clock clock;

    @Override
    public InventoryItemLocationAssignmentView registerAssignment(RegisterInventoryItemLocationAssignmentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryItem item = findItem(command.sku(), command.itemLocationCode());
        InventoryLocation assignedLocation = findActiveLocation(command.assignedLocationCode());
        String assignedFacilityCode = normalizeOptionalUpper(command.assignedFacilityCode(), "assignedFacilityCode");
        String assignedStorageAreaCode = normalizeOptionalUpper(command.assignedStorageAreaCode(), "assignedStorageAreaCode");
        validateStorageTarget(assignedFacilityCode, assignedStorageAreaCode);
        Instant validFrom = command.validFrom() == null ? Instant.now(clock) : command.validFrom();
        return toView(assignmentRepository.save(InventoryItemLocationAssignment.create(
            item,
            assignedLocation,
            assignedFacilityCode,
            assignedStorageAreaCode,
            validFrom,
            command.assignedBy(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemLocationAssignmentView assignmentById(UUID id) {
        return toView(findAssignment(id));
    }

    @Override
    public InventoryItemLocationAssignmentView endAssignment(UUID id, EndInventoryItemLocationAssignmentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        if (id == null || command.id() == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (!id.equals(command.id())) {
            throw new IllegalArgumentException("id path variable must match command id");
        }
        InventoryItemLocationAssignment assignment = findAssignment(id);
        Instant previousValidThru = assignment.getValidThru();
        Instant endedAt = Instant.now(clock);
        assignment.end(command.validThru(), command.reason(), command.endedBy(), endedAt);
        endAuditRepository.save(InventoryItemLocationAssignmentEndAudit.create(
            assignment,
            previousValidThru,
            command.reason(),
            command.endedBy(),
            endedAt
        ));
        return toView(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryItemLocationAssignmentEndView> listEndHistory(
        UUID id,
        String endedBy,
        Instant endedAtFrom,
        Instant endedAtTo,
        PageQuery pageQuery
    ) {
        InventoryItemLocationAssignment assignment = findAssignment(id);
        Page<InventoryItemLocationAssignmentEndAudit> history = endAuditRepository.findHistoryFiltered(
            assignment.getId(),
            normalizeOptionalLower(endedBy, "endedBy"),
            endedAtFrom,
            endedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "endedAt"))
        );
        return PageResult.from(history).map(this::toEndView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryItemLocationAssignmentView> listAssignments(
        String sku,
        String itemLocationCode,
        String assignedLocationCode,
        String assignedFacilityCode,
        String assignedStorageAreaCode,
        Boolean active,
        PageQuery pageQuery
    ) {
        Page<InventoryItemLocationAssignment> assignments = assignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(sku, "sku"),
            normalizeOptionalUpper(itemLocationCode, "itemLocationCode"),
            normalizeOptionalUpper(assignedLocationCode, "assignedLocationCode"),
            normalizeOptionalUpper(assignedFacilityCode, "assignedFacilityCode"),
            normalizeOptionalUpper(assignedStorageAreaCode, "assignedStorageAreaCode"),
            active,
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "sku").and(Sort.by("validFrom")))
        );
        return PageResult.from(assignments).map(this::toView);
    }

    private InventoryItem findItem(String sku, String itemLocationCode) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        String normalizedItemLocationCode = normalizeRequired(itemLocationCode, "itemLocationCode").toUpperCase();
        return inventoryItemRepository.findBySkuAndLocationCode(normalizedSku, normalizedItemLocationCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory item not found for SKU: "
                    + normalizedSku
                    + " at location: "
                    + normalizedItemLocationCode
            ));
    }

    private InventoryLocation findActiveLocation(String code) {
        String normalizedCode = normalizeRequired(code, "assignedLocationCode").toUpperCase();
        InventoryLocation location = inventoryLocationRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory location not found for code: " + normalizedCode));
        if (!location.isActive()) {
            throw new IllegalArgumentException("Inventory location is inactive: " + normalizedCode);
        }
        return location;
    }

    private void validateStorageTarget(String assignedFacilityCode, String assignedStorageAreaCode) {
        if (assignedStorageAreaCode != null && assignedFacilityCode == null) {
            throw new IllegalArgumentException("assignedFacilityCode is required when assignedStorageAreaCode is supplied");
        }
        if (assignedFacilityCode == null) {
            return;
        }
        InventoryFacility facility = inventoryFacilityRepository.findByCode(assignedFacilityCode)
            .orElseThrow(() -> new IllegalArgumentException("Inventory facility not found: " + assignedFacilityCode));
        if (!facility.isActive()) {
            throw new IllegalArgumentException("Inventory facility is inactive: " + assignedFacilityCode);
        }
        if (assignedStorageAreaCode != null) {
            InventoryStorageArea storageArea = inventoryStorageAreaRepository
                .findByFacilityCodeAndCode(assignedFacilityCode, assignedStorageAreaCode)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Inventory storage area not found for facility: "
                        + assignedFacilityCode
                        + " and code: "
                        + assignedStorageAreaCode
                ));
            if (!storageArea.isActive()) {
                throw new IllegalArgumentException(
                    "Inventory storage area is inactive for facility: "
                        + assignedFacilityCode
                        + " and code: "
                        + assignedStorageAreaCode
                );
            }
        }
    }

    private InventoryItemLocationAssignment findAssignment(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return assignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Inventory item location assignment not found for id: " + id));
    }

    private InventoryItemLocationAssignmentView toView(InventoryItemLocationAssignment assignment) {
        return new InventoryItemLocationAssignmentView(
            assignment.getId(),
            assignment.getInventoryItemId(),
            assignment.getSku(),
            assignment.getItemLocationCode(),
            assignment.getAssignedLocationCode(),
            assignment.getAssignedFacilityCode(),
            assignment.getAssignedStorageAreaCode(),
            assignment.getValidFrom(),
            assignment.getValidThru(),
            assignment.isActive(),
            assignment.getAssignedBy(),
            assignment.getAssignedAt(),
            assignment.getEndReason(),
            assignment.getEndedBy(),
            assignment.getEndedAt()
        );
    }

    private InventoryItemLocationAssignmentEndView toEndView(InventoryItemLocationAssignmentEndAudit audit) {
        return new InventoryItemLocationAssignmentEndView(
            audit.getId(),
            audit.getAssignmentId(),
            audit.getInventoryItemId(),
            audit.getSku(),
            audit.getItemLocationCode(),
            audit.getAssignedLocationCode(),
            audit.getAssignedFacilityCode(),
            audit.getAssignedStorageAreaCode(),
            audit.getPreviousValidThru(),
            audit.getCurrentValidThru(),
            audit.getReason(),
            audit.getEndedBy(),
            audit.getEndedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalUpper(String value, String fieldName) {
        return value == null ? null : normalizeRequired(value, fieldName).toUpperCase();
    }

    private static String normalizeOptionalLower(String value, String fieldName) {
        return value == null ? null : normalizeRequired(value, fieldName).toLowerCase();
    }
}
