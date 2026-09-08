package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.EndInventoryFixedAssetPartyRoleAssignmentCommand;
import com.arcanaerp.platform.inventory.InventoryFixedAssetPartyRoleAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetPartyRoleAssignmentEndView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetPartyRoleAssignmentView;
import com.arcanaerp.platform.inventory.InventoryPartyDirectory;
import com.arcanaerp.platform.inventory.InventoryPartyRoleTypeDirectory;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetPartyRoleAssignmentCommand;
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
class InventoryFixedAssetPartyRoleAssignmentDirectoryService
    implements InventoryFixedAssetPartyRoleAssignmentDirectory {

    private final InventoryFixedAssetPartyRoleAssignmentRepository assignmentRepository;
    private final InventoryFixedAssetPartyRoleAssignmentEndAuditRepository endAuditRepository;
    private final InventoryFixedAssetRepository inventoryFixedAssetRepository;
    private final InventoryPartyDirectory inventoryPartyDirectory;
    private final InventoryPartyRoleTypeDirectory inventoryPartyRoleTypeDirectory;
    private final Clock clock;

    @Override
    public InventoryFixedAssetPartyRoleAssignmentView registerAssignment(
        RegisterInventoryFixedAssetPartyRoleAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryFixedAsset fixedAsset = findFixedAsset(command.fixedAssetCode());
        if (!fixedAsset.isActive()) {
            throw new IllegalArgumentException("Inventory fixed asset is inactive: " + fixedAsset.getCode());
        }
        String partyCode = normalizeRequired(command.partyCode(), "partyCode").toUpperCase();
        String roleTypeCode = normalizeRequired(command.roleTypeCode(), "roleTypeCode").toUpperCase();
        ensurePartyExists(partyCode);
        ensureRoleTypeExists(roleTypeCode);
        if (
            assignmentRepository.findByInventoryFixedAssetIdAndPartyCodeAndRoleTypeCode(
                fixedAsset.getId(),
                partyCode,
                roleTypeCode
            ).isPresent()
        ) {
            throw new ConflictException(
                "Inventory fixed asset party role assignment already exists for fixed asset: "
                    + fixedAsset.getCode()
                    + ", party: "
                    + partyCode
                    + ", role type: "
                    + roleTypeCode
            );
        }
        return toView(assignmentRepository.save(InventoryFixedAssetPartyRoleAssignment.create(
            fixedAsset,
            partyCode,
            roleTypeCode,
            command.comments(),
            command.fromDate(),
            command.thruDate(),
            command.assignedBy(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryFixedAssetPartyRoleAssignmentView assignmentById(UUID id) {
        return toView(findAssignment(id));
    }

    @Override
    public InventoryFixedAssetPartyRoleAssignmentView endAssignment(
        UUID id,
        EndInventoryFixedAssetPartyRoleAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        if (id == null || command.id() == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (!id.equals(command.id())) {
            throw new IllegalArgumentException("id path variable must match command id");
        }
        InventoryFixedAssetPartyRoleAssignment assignment = findAssignment(id);
        Instant previousThruDate = assignment.getThruDate();
        Instant endedAt = Instant.now(clock);
        assignment.end(command.thruDate(), command.reason(), command.endedBy(), endedAt);
        endAuditRepository.save(InventoryFixedAssetPartyRoleAssignmentEndAudit.create(
            assignment,
            previousThruDate,
            command.reason(),
            command.endedBy(),
            endedAt
        ));
        return toView(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryFixedAssetPartyRoleAssignmentEndView> listEndHistory(
        UUID id,
        String endedBy,
        Instant endedAtFrom,
        Instant endedAtTo,
        PageQuery pageQuery
    ) {
        InventoryFixedAssetPartyRoleAssignment assignment = findAssignment(id);
        Page<InventoryFixedAssetPartyRoleAssignmentEndAudit> history = endAuditRepository.findHistoryFiltered(
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
    public PageResult<InventoryFixedAssetPartyRoleAssignmentView> listAssignments(
        String fixedAssetCode,
        String partyCode,
        String roleTypeCode,
        String assignedBy,
        Boolean active,
        PageQuery pageQuery
    ) {
        Page<InventoryFixedAssetPartyRoleAssignment> assignments = assignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(fixedAssetCode, "fixedAssetCode"),
            normalizeOptionalUpper(partyCode, "partyCode"),
            normalizeOptionalUpper(roleTypeCode, "roleTypeCode"),
            normalizeOptionalLower(assignedBy, "assignedBy"),
            active,
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "fixedAssetCode").and(Sort.by("partyCode")))
        );
        return PageResult.from(assignments).map(this::toView);
    }

    private InventoryFixedAsset findFixedAsset(String fixedAssetCode) {
        String normalizedFixedAssetCode = normalizeRequired(fixedAssetCode, "fixedAssetCode").toUpperCase();
        return inventoryFixedAssetRepository.findByCode(normalizedFixedAssetCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory fixed asset not found for code: " + normalizedFixedAssetCode
            ));
    }

    private InventoryFixedAssetPartyRoleAssignment findAssignment(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return assignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory fixed asset party role assignment not found for id: " + id
            ));
    }

    private InventoryFixedAssetPartyRoleAssignmentView toView(InventoryFixedAssetPartyRoleAssignment assignment) {
        return new InventoryFixedAssetPartyRoleAssignmentView(
            assignment.getId(),
            assignment.getInventoryFixedAssetId(),
            assignment.getFixedAssetCode(),
            assignment.getPartyCode(),
            assignment.getRoleTypeCode(),
            assignment.getComments(),
            assignment.getFromDate(),
            assignment.getThruDate(),
            assignment.getAssignedBy(),
            assignment.getAssignedAt(),
            assignment.isActive(),
            assignment.getEndReason(),
            assignment.getEndedBy(),
            assignment.getEndedAt()
        );
    }

    private InventoryFixedAssetPartyRoleAssignmentEndView toEndView(
        InventoryFixedAssetPartyRoleAssignmentEndAudit audit
    ) {
        return new InventoryFixedAssetPartyRoleAssignmentEndView(
            audit.getId(),
            audit.getAssignmentId(),
            audit.getInventoryFixedAssetId(),
            audit.getFixedAssetCode(),
            audit.getPartyCode(),
            audit.getRoleTypeCode(),
            audit.getPreviousThruDate(),
            audit.getCurrentThruDate(),
            audit.getReason(),
            audit.getEndedBy(),
            audit.getEndedAt()
        );
    }

    private void ensurePartyExists(String partyCode) {
        if (!inventoryPartyDirectory.partyExists(partyCode)) {
            throw new IllegalArgumentException("Inventory party not found: " + partyCode);
        }
    }

    private void ensureRoleTypeExists(String roleTypeCode) {
        if (!inventoryPartyRoleTypeDirectory.roleTypeExists(roleTypeCode)) {
            throw new IllegalArgumentException("Inventory party role type not found: " + roleTypeCode);
        }
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
