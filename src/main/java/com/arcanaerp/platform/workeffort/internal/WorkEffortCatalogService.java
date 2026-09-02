package com.arcanaerp.platform.workeffort.internal;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import com.arcanaerp.platform.workeffort.AssignWorkEffortCommand;
import com.arcanaerp.platform.workeffort.ChangeWorkEffortStatusCommand;
import com.arcanaerp.platform.workeffort.CreateWorkEffortCommand;
import com.arcanaerp.platform.workeffort.DailyWorkEffortAssignmentActivityByAssigneeSummaryView;
import com.arcanaerp.platform.workeffort.DailyWorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.DailyWorkEffortStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.workeffort.DailyWorkEffortStatusActivitySummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortAssignmentActivityByAssigneeSummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.workeffort.MonthlyWorkEffortStatusActivitySummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortAssignmentActivityByAssigneeSummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.workeffort.WeeklyWorkEffortStatusActivitySummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentActivitySummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentChangeView;
import com.arcanaerp.platform.workeffort.WorkEffortAssignmentSummaryView;
import com.arcanaerp.platform.workeffort.WorkEffortCatalog;
import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import com.arcanaerp.platform.workeffort.WorkEffortStatusChangeView;
import com.arcanaerp.platform.workeffort.WorkEffortView;
import java.time.DayOfWeek;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
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
    @Transactional(readOnly = true)
    public PageResult<WorkEffortAssignmentActivitySummaryView> listAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        Page<WorkEffortAssignmentChangeAuditRepository.AssignmentActivitySummaryProjection> page =
            workEffortAssignmentChangeAuditRepository.summarizeAssignmentActivity(
                normalizedTenantCode,
                normalizedAssignedTo,
                assignedAtFrom,
                assignedAtTo,
                pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "currentAssignedTo"))
            );
        return PageResult.from(page).map(summary -> new WorkEffortAssignmentActivitySummaryView(
            normalizedTenantCode,
            summary.getAssignedTo(),
            summary.getAssignmentCount(),
            summary.getFirstAssignedAt(),
            summary.getLastAssignedAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyWorkEffortAssignmentActivitySummaryView> listDailyAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucket(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyWorkEffortAssignmentActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyWorkEffortAssignmentActivitySummaryView> listWeeklyAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucket(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyWorkEffortAssignmentActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyWorkEffortAssignmentActivityByAssigneeSummaryView> listDailyAssignmentActivityByAssigneeSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucketAndAssignee(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyWorkEffortAssignmentActivityByAssigneeSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyWorkEffortAssignmentActivityByAssigneeSummaryView> listWeeklyAssignmentActivityByAssigneeSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucketAndAssignee(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyWorkEffortAssignmentActivityByAssigneeSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyWorkEffortAssignmentActivitySummaryView> listMonthlyAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucket(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getAssignedAt().atOffset(ZoneOffset.UTC)),
            MonthlyWorkEffortAssignmentActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyWorkEffortAssignmentActivityByAssigneeSummaryView> listMonthlyAssignmentActivityByAssigneeSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAssignmentActivityByBucketAndAssignee(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getAssignedAt().atOffset(ZoneOffset.UTC)),
            MonthlyWorkEffortAssignmentActivityByAssigneeSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyWorkEffortStatusActivitySummaryView> listDailyStatusActivitySummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyWorkEffortStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyWorkEffortStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyWorkEffortStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyWorkEffortStatusActivityByCurrentStatusSummaryView> listDailyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucketAndCurrentStatus(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyWorkEffortStatusActivityByCurrentStatusSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView> listWeeklyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucketAndCurrentStatus(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyWorkEffortStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getChangedAt().atOffset(ZoneOffset.UTC)),
            MonthlyWorkEffortStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView> listMonthlyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucketAndCurrentStatus(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getChangedAt().atOffset(ZoneOffset.UTC)),
            MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView::new
        );
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

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeAssignmentActivityByBucket(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery,
        AssignmentBucketExtractor<B> bucketExtractor,
        AssignmentBucketSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        List<WorkEffortAssignmentChangeAudit> audits = workEffortAssignmentChangeAuditRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            normalizedAssignedTo,
            assignedAtFrom,
            assignedAtTo
        );
        TreeMap<B, AssignmentBucketSummary> summaries = new TreeMap<>(java.util.Comparator.reverseOrder());
        for (WorkEffortAssignmentChangeAudit audit : audits) {
            B bucket = bucketExtractor.bucket(audit);
            AssignmentBucketSummary summary = summaries.computeIfAbsent(bucket, ignored -> new AssignmentBucketSummary());
            summary.assignmentCount++;
            summary.workEffortIds.add(audit.getWorkEffortId());
        }
        List<T> rows = summaries.entrySet().stream()
            .map(entry -> summaryFactory.create(
                normalizedTenantCode,
                entry.getKey(),
                entry.getValue().assignmentCount,
                entry.getValue().workEffortIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeAssignmentActivityByBucketAndAssignee(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery,
        AssignmentBucketExtractor<B> bucketExtractor,
        AssignmentBucketAssigneeSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeAssignedTo(assignedTo);
        List<WorkEffortAssignmentChangeAudit> audits = workEffortAssignmentChangeAuditRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            normalizedAssignedTo,
            assignedAtFrom,
            assignedAtTo
        );
        Map<AssignmentBucketAssigneeKey<B>, AssignmentBucketSummary> summaries = new java.util.HashMap<>();
        for (WorkEffortAssignmentChangeAudit audit : audits) {
            AssignmentBucketAssigneeKey<B> key = new AssignmentBucketAssigneeKey<>(
                bucketExtractor.bucket(audit),
                audit.getCurrentAssignedTo()
            );
            AssignmentBucketSummary summary = summaries.computeIfAbsent(key, ignored -> new AssignmentBucketSummary());
            summary.assignmentCount++;
            summary.workEffortIds.add(audit.getWorkEffortId());
        }
        List<T> rows = summaries.entrySet().stream()
            .sorted((left, right) -> {
                int bucketComparison = right.getKey().bucket().compareTo(left.getKey().bucket());
                if (bucketComparison != 0) {
                    return bucketComparison;
                }
                return left.getKey().assignedTo().compareTo(right.getKey().assignedTo());
            })
            .map(entry -> summaryFactory.create(
                normalizedTenantCode,
                entry.getKey().bucket(),
                entry.getKey().assignedTo(),
                entry.getValue().assignmentCount,
                entry.getValue().workEffortIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeStatusActivityByBucket(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery,
        StatusBucketExtractor<B> bucketExtractor,
        StatusBucketSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedChangedBy = changedBy == null ? null : normalizeActorEmail(changedBy, "changedBy");
        List<WorkEffortStatusChangeAudit> audits = workEffortStatusChangeAuditRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo
        );
        TreeMap<B, StatusBucketSummary> summaries = new TreeMap<>(java.util.Comparator.reverseOrder());
        for (WorkEffortStatusChangeAudit audit : audits) {
            B bucket = bucketExtractor.bucket(audit);
            StatusBucketSummary summary = summaries.computeIfAbsent(bucket, ignored -> new StatusBucketSummary());
            summary.transitionCount++;
            summary.workEffortIds.add(audit.getWorkEffortId());
        }
        List<T> rows = summaries.entrySet().stream()
            .map(entry -> summaryFactory.create(
                normalizedTenantCode,
                entry.getKey(),
                entry.getValue().transitionCount,
                entry.getValue().workEffortIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeStatusActivityByBucketAndCurrentStatus(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery,
        StatusBucketExtractor<B> bucketExtractor,
        StatusBucketStatusSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedChangedBy = changedBy == null ? null : normalizeActorEmail(changedBy, "changedBy");
        List<WorkEffortStatusChangeAudit> audits = workEffortStatusChangeAuditRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo
        );
        Map<StatusBucketStatusKey<B>, StatusBucketSummary> summaries = new java.util.HashMap<>();
        for (WorkEffortStatusChangeAudit audit : audits) {
            StatusBucketStatusKey<B> key = new StatusBucketStatusKey<>(bucketExtractor.bucket(audit), audit.getCurrentStatus());
            StatusBucketSummary summary = summaries.computeIfAbsent(key, ignored -> new StatusBucketSummary());
            summary.transitionCount++;
            summary.workEffortIds.add(audit.getWorkEffortId());
        }
        List<T> rows = summaries.entrySet().stream()
            .sorted((left, right) -> {
                int bucketComparison = right.getKey().bucket().compareTo(left.getKey().bucket());
                if (bucketComparison != 0) {
                    return bucketComparison;
                }
                return left.getKey().currentStatus().compareTo(right.getKey().currentStatus());
            })
            .map(entry -> summaryFactory.create(
                normalizedTenantCode,
                entry.getKey().bucket(),
                entry.getKey().currentStatus(),
                entry.getValue().transitionCount,
                entry.getValue().workEffortIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private static <T> PageResult<T> paginate(List<T> rows, PageQuery pageQuery) {
        int fromIndex = Math.min(pageQuery.page() * pageQuery.size(), rows.size());
        int toIndex = Math.min(fromIndex + pageQuery.size(), rows.size());
        List<T> pageRows = new ArrayList<>(rows.subList(fromIndex, toIndex));
        int totalPages = rows.isEmpty() ? 0 : (int) Math.ceil((double) rows.size() / pageQuery.size());
        return new PageResult<>(
            pageRows,
            pageQuery.page(),
            pageQuery.size(),
            rows.size(),
            totalPages,
            pageQuery.page() + 1 < totalPages,
            pageQuery.page() > 0 && !rows.isEmpty()
        );
    }

    @FunctionalInterface
    private interface AssignmentBucketExtractor<B> {
        B bucket(WorkEffortAssignmentChangeAudit audit);
    }

    @FunctionalInterface
    private interface AssignmentBucketSummaryFactory<B, T> {
        T create(String tenantCode, B bucket, long assignmentCount, long workEffortCount);
    }

    @FunctionalInterface
    private interface AssignmentBucketAssigneeSummaryFactory<B, T> {
        T create(String tenantCode, B bucket, String assignedTo, long assignmentCount, long workEffortCount);
    }

    private record AssignmentBucketAssigneeKey<B extends Comparable<? super B>>(B bucket, String assignedTo) {
    }

    @FunctionalInterface
    private interface StatusBucketExtractor<B> {
        B bucket(WorkEffortStatusChangeAudit audit);
    }

    @FunctionalInterface
    private interface StatusBucketSummaryFactory<B, T> {
        T create(String tenantCode, B bucket, long transitionCount, long workEffortCount);
    }

    @FunctionalInterface
    private interface StatusBucketStatusSummaryFactory<B, T> {
        T create(String tenantCode, B bucket, WorkEffortStatus currentStatus, long transitionCount, long workEffortCount);
    }

    private record StatusBucketStatusKey<B extends Comparable<? super B>>(B bucket, WorkEffortStatus currentStatus) {
    }

    private static final class AssignmentBucketSummary {
        private long assignmentCount;
        private final Set<UUID> workEffortIds = new HashSet<>();
    }

    private static final class StatusBucketSummary {
        private long transitionCount;
        private final Set<UUID> workEffortIds = new HashSet<>();
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
