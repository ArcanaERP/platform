package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.EndInventoryFixedAssetFacilityAssignmentCommand;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentEndView;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentTypeDirectory;
import com.arcanaerp.platform.inventory.InventoryFixedAssetFacilityAssignmentView;
import com.arcanaerp.platform.inventory.RegisterInventoryFixedAssetFacilityAssignmentCommand;
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
class InventoryFixedAssetFacilityAssignmentDirectoryService
    implements InventoryFixedAssetFacilityAssignmentDirectory {

    private final InventoryFixedAssetFacilityAssignmentRepository assignmentRepository;
    private final InventoryFixedAssetFacilityAssignmentEndAuditRepository endAuditRepository;
    private final InventoryFixedAssetRepository inventoryFixedAssetRepository;
    private final InventoryFacilityRepository inventoryFacilityRepository;
    private final InventoryFixedAssetFacilityAssignmentTypeDirectory assignmentTypeDirectory;
    private final Clock clock;

    @Override
    public InventoryFixedAssetFacilityAssignmentView registerAssignment(
        RegisterInventoryFixedAssetFacilityAssignmentCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        InventoryFixedAsset fixedAsset = findFixedAsset(command.fixedAssetCode());
        if (!fixedAsset.isActive()) {
            throw new IllegalArgumentException("Inventory fixed asset is inactive: " + fixedAsset.getCode());
        }
        InventoryFacility facility = findFacility(command.facilityCode());
        if (!facility.isActive()) {
            throw new IllegalArgumentException("Inventory facility is inactive: " + facility.getCode());
        }
        String assignmentType = normalizeRequired(command.assignmentType(), "assignmentType").toUpperCase();
        ensureAssignmentTypeExists(assignmentType);
        if (
            assignmentRepository.findByInventoryFixedAssetIdAndInventoryFacilityIdAndAssignmentType(
                fixedAsset.getId(),
                facility.getId(),
                assignmentType
            ).isPresent()
        ) {
            throw new ConflictException(
                "Inventory fixed asset facility assignment already exists for fixed asset: "
                    + fixedAsset.getCode()
                    + ", facility: "
                    + facility.getCode()
                    + ", assignment type: "
                    + assignmentType
            );
        }
        return toView(assignmentRepository.save(InventoryFixedAssetFacilityAssignment.create(
            fixedAsset,
            facility,
            assignmentType,
            command.comments(),
            command.fromDate(),
            command.thruDate(),
            command.assignedBy(),
            Instant.now(clock)
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryFixedAssetFacilityAssignmentView assignmentById(UUID id) {
        return toView(findAssignment(id));
    }

    @Override
    public InventoryFixedAssetFacilityAssignmentView endAssignment(
        UUID id,
        EndInventoryFixedAssetFacilityAssignmentCommand command
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
        InventoryFixedAssetFacilityAssignment assignment = findAssignment(id);
        Instant previousThruDate = assignment.getThruDate();
        Instant endedAt = Instant.now(clock);
        assignment.end(command.thruDate(), command.reason(), command.endedBy(), endedAt);
        endAuditRepository.save(InventoryFixedAssetFacilityAssignmentEndAudit.create(
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
    public PageResult<InventoryFixedAssetFacilityAssignmentEndView> listEndHistory(
        UUID id,
        String endedBy,
        Instant endedAtFrom,
        Instant endedAtTo,
        PageQuery pageQuery
    ) {
        InventoryFixedAssetFacilityAssignment assignment = findAssignment(id);
        Page<InventoryFixedAssetFacilityAssignmentEndAudit> history = endAuditRepository.findHistoryFiltered(
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
    public PageResult<InventoryFixedAssetFacilityAssignmentView> listAssignments(
        String fixedAssetCode,
        String facilityCode,
        String assignmentType,
        String assignedBy,
        Boolean active,
        PageQuery pageQuery
    ) {
        Page<InventoryFixedAssetFacilityAssignment> assignments = assignmentRepository.findAssignmentsFiltered(
            normalizeOptionalUpper(fixedAssetCode, "fixedAssetCode"),
            normalizeOptionalUpper(facilityCode, "facilityCode"),
            normalizeOptionalUpper(assignmentType, "assignmentType"),
            normalizeOptionalLower(assignedBy, "assignedBy"),
            active,
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "fixedAssetCode").and(Sort.by("facilityCode")))
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

    private InventoryFacility findFacility(String facilityCode) {
        String normalizedFacilityCode = normalizeRequired(facilityCode, "facilityCode").toUpperCase();
        return inventoryFacilityRepository.findByCode(normalizedFacilityCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory facility not found for code: " + normalizedFacilityCode
            ));
    }

    private InventoryFixedAssetFacilityAssignment findAssignment(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return assignmentRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory fixed asset facility assignment not found for id: " + id
            ));
    }

    private void ensureAssignmentTypeExists(String assignmentType) {
        if (!assignmentTypeDirectory.assignmentTypeExists(assignmentType)) {
            throw new IllegalArgumentException(
                "Inventory fixed asset facility assignment type not found for code: " + assignmentType
            );
        }
    }

    private InventoryFixedAssetFacilityAssignmentView toView(InventoryFixedAssetFacilityAssignment assignment) {
        return new InventoryFixedAssetFacilityAssignmentView(
            assignment.getId(),
            assignment.getInventoryFixedAssetId(),
            assignment.getInventoryFacilityId(),
            assignment.getFixedAssetCode(),
            assignment.getFacilityCode(),
            assignment.getAssignmentType(),
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

    private InventoryFixedAssetFacilityAssignmentEndView toEndView(
        InventoryFixedAssetFacilityAssignmentEndAudit audit
    ) {
        return new InventoryFixedAssetFacilityAssignmentEndView(
            audit.getId(),
            audit.getAssignmentId(),
            audit.getInventoryFixedAssetId(),
            audit.getInventoryFacilityId(),
            audit.getFixedAssetCode(),
            audit.getFacilityCode(),
            audit.getAssignmentType(),
            audit.getPreviousThruDate(),
            audit.getCurrentThruDate(),
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
