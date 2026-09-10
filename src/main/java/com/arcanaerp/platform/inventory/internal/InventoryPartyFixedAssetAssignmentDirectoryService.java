package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.InventoryPartyFixedAssetAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryPartyFixedAssetAssignmentView;
import com.arcanaerp.platform.inventory.RegisterInventoryPartyFixedAssetAssignmentCommand;
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
class InventoryPartyFixedAssetAssignmentDirectoryService implements InventoryPartyFixedAssetAssignmentDirectory {

    private final InventoryPartyFixedAssetAssignmentRepository assignmentRepository;
    private final InventoryPartyRepository inventoryPartyRepository;
    private final InventoryFixedAssetRepository inventoryFixedAssetRepository;
    private final Clock clock;

    @Override
    public InventoryPartyFixedAssetAssignmentView registerAssignment(
        RegisterInventoryPartyFixedAssetAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryParty party = findParty(command.partyCode());
        InventoryFixedAsset fixedAsset = findFixedAsset(command.fixedAssetCode());
        return toView(assignmentRepository.save(InventoryPartyFixedAssetAssignment.create(
            party,
            fixedAsset,
            command.assignedFrom(),
            command.assignedThru(),
            command.allocatedCostMoneyId(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryPartyFixedAssetAssignmentView assignmentById(UUID id) {
        return toView(findAssignment(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryPartyFixedAssetAssignmentView> listAssignments(
        String partyCode,
        String fixedAssetCode,
        Long allocatedCostMoneyId,
        PageQuery pageQuery
    ) {
        Page<InventoryPartyFixedAssetAssignment> assignments = assignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(partyCode, "partyCode"),
            normalizeOptionalUpper(fixedAssetCode, "fixedAssetCode"),
            allocatedCostMoneyId,
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "partyCode").and(Sort.by("fixedAssetCode")))
        );
        return PageResult.from(assignments).map(this::toView);
    }

    private InventoryParty findParty(String partyCode) {
        String normalizedPartyCode = normalizeRequired(partyCode, "partyCode").toUpperCase();
        return inventoryPartyRepository.findByCode(normalizedPartyCode)
            .orElseThrow(() -> new NoSuchElementException("Inventory party not found for code: " + normalizedPartyCode));
    }

    private InventoryFixedAsset findFixedAsset(String fixedAssetCode) {
        String normalizedFixedAssetCode = normalizeRequired(fixedAssetCode, "fixedAssetCode").toUpperCase();
        return inventoryFixedAssetRepository.findByCode(normalizedFixedAssetCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory fixed asset not found for code: " + normalizedFixedAssetCode
            ));
    }

    private InventoryPartyFixedAssetAssignment findAssignment(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return assignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory party fixed asset assignment not found for id: " + id
            ));
    }

    private InventoryPartyFixedAssetAssignmentView toView(InventoryPartyFixedAssetAssignment assignment) {
        return new InventoryPartyFixedAssetAssignmentView(
            assignment.getId(),
            assignment.getInventoryPartyId(),
            assignment.getPartyCode(),
            assignment.getInventoryFixedAssetId(),
            assignment.getFixedAssetCode(),
            assignment.getAssignedFrom(),
            assignment.getAssignedThru(),
            assignment.getAllocatedCostMoneyId(),
            assignment.getCreatedAt()
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
}
