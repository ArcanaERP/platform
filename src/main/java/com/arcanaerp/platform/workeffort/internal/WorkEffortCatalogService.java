package com.arcanaerp.platform.workeffort.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import com.arcanaerp.platform.workeffort.AssignWorkEffortCommand;
import com.arcanaerp.platform.workeffort.ChangeWorkEffortStatusCommand;
import com.arcanaerp.platform.workeffort.CreateWorkEffortCommand;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentChangeView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentSummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortCatalog;
import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import com.arcanaerp.platform.workeffort.WorkEffortStatusChangeView;
import com.arcanaerp.platform.workeffort.WorkEffortView;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class WorkEffortCatalogService implements WorkEffortCatalog {

    private final WorkEffortRepository workEffortRepository;
    private final WorkEffortStatusChangeAuditRepository workEffortStatusChangeAuditRepository;
    private final WorkEffortAssignmentChangeAuditRepository workEffortAssignmentChangeAuditRepository;
    private final IdentityActorLookup identityActorLookup;
    private final Clock clock;

    @Override
    public WorkEffortView createWorkEffort(CreateWorkEffortCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String effortNumber = normalizeRequired(command.effortNumber(), "effortNumber").toUpperCase();
        String assignedTo = normalizeAssignedTo(command.assignedTo());
        Instant now = Instant.now(clock);

        if (workEffortRepository.findByTenantCodeAndEffortNumber(tenantCode, effortNumber).isPresent()) {
            throw new ConflictException("Work effort already exists for tenant/effortNumber: " + tenantCode + "/" + effortNumber);
        }
        if (!identityActorLookup.actorExists(tenantCode, assignedTo)) {
            throw new IllegalArgumentException("work effort assignee not found in tenant: " + tenantCode + "/" + assignedTo);
        }

        WorkEffort created = workEffortRepository.save(
            WorkEffort.create(
                tenantCode,
                effortNumber,
                command.name(),
                command.description(),
                command.status(),
                assignedTo,
                command.dueAt(),
                now
            )
        );
        return toView(created);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortView getWorkEffort(String tenantCode, String effortNumber) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedEffortNumber = normalizeRequired(effortNumber, "effortNumber").toUpperCase();
        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(normalizedTenantCode, normalizedEffortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + normalizedTenantCode + "/" + normalizedEffortNumber
            ));
        return toView(workEffort);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkEffortAssignmentSummaryView getWorkEffortAssignment(String tenantCode, String effortNumber) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedEffortNumber = normalizeRequired(effortNumber, "effortNumber").toUpperCase();
        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(normalizedTenantCode, normalizedEffortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + normalizedTenantCode + "/" + normalizedEffortNumber
            ));
        return toAssignmentSummaryView(workEffort);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortView> listWorkEfforts(
        String tenantCode,
        PageQuery pageQuery,
        WorkEffortStatus status,
        String assignedTo
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        Page<WorkEffort> page = findWorkEfforts(normalizedTenantCode, status, normalizedAssignedTo, pageQuery);
        return PageResult.from(page).map(this::toView);
    }

    @Override
    public WorkEffortView changeWorkEffortStatus(ChangeWorkEffortStatusCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String effortNumber = normalizeRequired(command.effortNumber(), "effortNumber").toUpperCase();
        WorkEffortStatus targetStatus = command.status();
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeAssignedTo(command.changedBy());

        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(tenantCode, effortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + tenantCode + "/" + effortNumber
            ));
        if (!identityActorLookup.actorExists(tenantCode, changedBy)) {
            throw new IllegalArgumentException("work effort status actor not found in tenant: " + tenantCode + "/" + changedBy);
        }

        WorkEffortStatus previousStatus = workEffort.getStatus();
        workEffort.transitionTo(targetStatus);
        WorkEffort saved = workEffortRepository.save(workEffort);
        if (previousStatus != saved.getStatus()) {
            workEffortStatusChangeAuditRepository.save(
                WorkEffortStatusChangeAudit.create(
                    saved.getId(),
                    previousStatus,
                    saved.getStatus(),
                    tenantCode,
                    reason,
                    changedBy,
                    Instant.now(clock)
                )
            );
        }
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortStatusChangeView> listStatusHistory(
        String tenantCode,
        String effortNumber,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedEffortNumber = normalizeRequired(effortNumber, "effortNumber").toUpperCase();
        String normalizedChangedBy = changedBy == null ? null : normalizeAssignedTo(changedBy);

        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(normalizedTenantCode, normalizedEffortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + normalizedTenantCode + "/" + normalizedEffortNumber
            ));
        Page<WorkEffortStatusChangeAudit> page = workEffortStatusChangeAuditRepository.findHistoryFiltered(
            workEffort.getId(),
            normalizedTenantCode,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(page).map(audit -> new WorkEffortStatusChangeView(
            audit.getId(),
            workEffort.getEffortNumber(),
            audit.getPreviousStatus(),
            audit.getCurrentStatus(),
            audit.getTenantCode(),
            audit.getReason(),
            audit.getChangedBy(),
            audit.getChangedAt()
        ));
    }

    @Override
    public WorkEffortView assignWorkEffort(AssignWorkEffortCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String effortNumber = normalizeRequired(command.effortNumber(), "effortNumber").toUpperCase();
        String assignedTo = normalizeAssignedTo(command.assignedTo());
        String assignedBy = normalizeActorEmail(command.assignedBy(), "assignedBy");
        String reason = normalizeRequired(command.reason(), "reason");

        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(tenantCode, effortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + tenantCode + "/" + effortNumber
            ));
        if (!identityActorLookup.actorExists(tenantCode, assignedTo)) {
            throw new IllegalArgumentException("work effort assignee not found in tenant: " + tenantCode + "/" + assignedTo);
        }
        if (!identityActorLookup.actorExists(tenantCode, assignedBy)) {
            throw new IllegalArgumentException("work effort assignment actor not found in tenant: " + tenantCode + "/" + assignedBy);
        }

        String previousAssignedTo = workEffort.getAssignedTo();
        workEffort.assignTo(assignedTo);
        WorkEffort saved = workEffortRepository.save(workEffort);
        if (!previousAssignedTo.equals(saved.getAssignedTo())) {
            workEffortAssignmentChangeAuditRepository.save(
                WorkEffortAssignmentChangeAudit.create(
                    saved.getId(),
                    previousAssignedTo,
                    saved.getAssignedTo(),
                    tenantCode,
                    reason,
                    assignedBy,
                    Instant.now(clock)
                )
            );
        }
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WorkEffortAssignmentChangeView> listAssignmentHistory(
        String tenantCode,
        String effortNumber,
        String assignedTo,
        String assignedBy,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedEffortNumber = normalizeRequired(effortNumber, "effortNumber").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        String normalizedAssignedBy = assignedBy == null ? null : normalizeActorEmail(assignedBy, "assignedBy");

        WorkEffort workEffort = workEffortRepository.findByTenantCodeAndEffortNumber(normalizedTenantCode, normalizedEffortNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "Work effort not found for tenant/effortNumber: " + normalizedTenantCode + "/" + normalizedEffortNumber
            ));
        Page<WorkEffortAssignmentChangeAudit> page = workEffortAssignmentChangeAuditRepository.findHistoryFiltered(
            workEffort.getId(),
            normalizedTenantCode,
            normalizedAssignedTo,
            normalizedAssignedBy,
            assignedAtFrom,
            assignedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "assignedAt"))
        );
        return PageResult.from(page).map(audit -> new WorkEffortAssignmentChangeView(
            audit.getId(),
            workEffort.getEffortNumber(),
            audit.getPreviousAssignedTo(),
            audit.getCurrentAssignedTo(),
            audit.getTenantCode(),
            audit.getReason(),
            audit.getAssignedBy(),
            audit.getAssignedAt()
        ));
    }

    private Page<WorkEffort> findWorkEfforts(
        String tenantCode,
        WorkEffortStatus status,
        String assignedTo,
        PageQuery pageQuery
    ) {
        var pageable = pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null && assignedTo != null) {
            return workEffortRepository.findByTenantCodeAndStatusAndAssignedTo(tenantCode, status, assignedTo, pageable);
        }
        if (status != null) {
            return workEffortRepository.findByTenantCodeAndStatus(tenantCode, status, pageable);
        }
        if (assignedTo != null) {
            return workEffortRepository.findByTenantCodeAndAssignedTo(tenantCode, assignedTo, pageable);
        }
        return workEffortRepository.findByTenantCode(tenantCode, pageable);
    }

    private WorkEffortView toView(WorkEffort workEffort) {
        return new WorkEffortView(
            workEffort.getId(),
            workEffort.getTenantCode(),
            workEffort.getEffortNumber(),
            workEffort.getName(),
            workEffort.getDescription(),
            workEffort.getStatus(),
            workEffort.getAssignedTo(),
            workEffort.getDueAt(),
            workEffort.getCreatedAt()
        );
    }

    private WorkEffortAssignmentSummaryView toAssignmentSummaryView(WorkEffort workEffort) {
        return new WorkEffortAssignmentSummaryView(
            workEffort.getId(),
            workEffort.getTenantCode(),
            workEffort.getEffortNumber(),
            workEffort.getAssignedTo()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeAssignedTo(String assignedTo) {
        return normalizeActorEmail(assignedTo, "assignedTo");
    }

    private static String normalizeActorEmail(String value, String fieldName) {
        String normalized = normalizeRequired(value, fieldName).toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }
}
