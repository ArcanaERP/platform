package com.arcanaerp.platform.workeffort.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import com.arcanaerp.platform.workeffort.CreateWorkEffortCommand;
import com.arcanaerp.platform.workeffort.WorkEffortCatalog;
import com.arcanaerp.platform.workeffort.WorkEffortStatus;
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

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeAssignedTo(String assignedTo) {
        String normalized = normalizeRequired(assignedTo, "assignedTo").toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("assignedTo is invalid");
        }
        return normalized;
    }
}
