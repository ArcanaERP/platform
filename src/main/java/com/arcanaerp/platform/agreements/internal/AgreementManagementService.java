package com.arcanaerp.platform.agreements.internal;

import com.arcanaerp.platform.agreements.AgreementManagement;
import com.arcanaerp.platform.agreements.AgreementItemTypeView;
import com.arcanaerp.platform.agreements.AgreementItemView;
import com.arcanaerp.platform.agreements.AgreementStatus;
import com.arcanaerp.platform.agreements.AgreementStatusChangeView;
import com.arcanaerp.platform.agreements.AgreementTypeView;
import com.arcanaerp.platform.agreements.AgreementView;
import com.arcanaerp.platform.agreements.ChangeAgreementStatusCommand;
import com.arcanaerp.platform.agreements.CreateAgreementCommand;
import com.arcanaerp.platform.agreements.DailyAgreementStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.agreements.DailyAgreementStatusActivitySummaryView;
import com.arcanaerp.platform.agreements.MonthlyAgreementStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.agreements.MonthlyAgreementStatusActivitySummaryView;
import com.arcanaerp.platform.agreements.RegisterAgreementItemCommand;
import com.arcanaerp.platform.agreements.RegisterAgreementItemTypeCommand;
import com.arcanaerp.platform.agreements.RegisterAgreementTypeCommand;
import com.arcanaerp.platform.agreements.WeeklyAgreementStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.agreements.WeeklyAgreementStatusActivitySummaryView;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
class AgreementManagementService implements AgreementManagement {

    private final AgreementRepository agreementRepository;
    private final AgreementTypeRepository agreementTypeRepository;
    private final AgreementItemTypeRepository agreementItemTypeRepository;
    private final AgreementItemRepository agreementItemRepository;
    private final AgreementStatusChangeAuditRepository agreementStatusChangeAuditRepository;
    private final IdentityActorLookup identityActorLookup;
    private final Clock clock;

    @Override
    public AgreementView createAgreement(CreateAgreementCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        String normalizedAgreementNumber = normalizeRequired(command.agreementNumber(), "agreementNumber").toUpperCase();
        if (agreementRepository.findByAgreementNumber(normalizedAgreementNumber).isPresent()) {
            throw new IllegalArgumentException("Agreement number already exists: " + normalizedAgreementNumber);
        }

        Agreement agreement = agreementRepository.save(
            Agreement.create(
                command.tenantCode(),
                normalizedAgreementNumber,
                command.name(),
                command.agreementType(),
                command.effectiveFrom(),
                Instant.now(clock)
            )
        );
        return toView(agreement);
    }

    @Override
    public AgreementTypeView registerAgreementType(RegisterAgreementTypeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        return toAgreementTypeView(agreementTypeRepository.save(
            AgreementType.create(
                command.parentId(),
                command.left(),
                command.right(),
                command.description(),
                command.comments(),
                command.internalIdentifier(),
                command.externalIdentifier(),
                command.externalIdSource(),
                Instant.now(clock)
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementTypeView agreementTypeById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toAgreementTypeView(agreementTypeRepository.findById(id)
            .orElseThrow(() -> new java.util.NoSuchElementException("Agreement type not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgreementTypeView> listAgreementTypes(
        Long parentId,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        PageQuery pageQuery
    ) {
        Page<AgreementType> page = agreementTypeRepository.findTypesFiltered(
            parentId,
            normalizeOptional(internalIdentifier, "internalIdentifier"),
            normalizeOptional(externalIdentifier, "externalIdentifier"),
            normalizeOptional(externalIdSource, "externalIdSource"),
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toAgreementTypeView);
    }

    @Override
    public AgreementItemTypeView registerAgreementItemType(RegisterAgreementItemTypeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        return toAgreementItemTypeView(agreementItemTypeRepository.save(
            AgreementItemType.create(
                command.parentId(),
                command.left(),
                command.right(),
                command.description(),
                command.comments(),
                command.internalIdentifier(),
                command.externalIdentifier(),
                command.externalIdSource(),
                Instant.now(clock)
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementItemTypeView agreementItemTypeById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toAgreementItemTypeView(agreementItemTypeRepository.findById(id)
            .orElseThrow(() -> new java.util.NoSuchElementException("Agreement item type not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgreementItemTypeView> listAgreementItemTypes(
        Long parentId,
        String internalIdentifier,
        String externalIdentifier,
        String externalIdSource,
        PageQuery pageQuery
    ) {
        Page<AgreementItemType> page = agreementItemTypeRepository.findTypesFiltered(
            parentId,
            normalizeOptional(internalIdentifier, "internalIdentifier"),
            normalizeOptional(externalIdentifier, "externalIdentifier"),
            normalizeOptional(externalIdSource, "externalIdSource"),
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toAgreementItemTypeView);
    }

    @Override
    public AgreementItemView registerAgreementItem(RegisterAgreementItemCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        if (command.agreementId() != null && !agreementRepository.existsById(command.agreementId())) {
            throw new java.util.NoSuchElementException("Agreement not found for id: " + command.agreementId());
        }
        if (command.agreementItemTypeId() != null
            && !agreementItemTypeRepository.existsById(command.agreementItemTypeId())) {
            throw new java.util.NoSuchElementException(
                "Agreement item type not found for id: " + command.agreementItemTypeId()
            );
        }
        return toAgreementItemView(agreementItemRepository.save(
            AgreementItem.create(
                command.agreementId(),
                command.agreementItemTypeId(),
                command.agreementItemValue(),
                command.description(),
                command.agreementItemRuleString(),
                Instant.now(clock)
            )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementItemView agreementItemById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        return toAgreementItemView(agreementItemRepository.findById(id)
            .orElseThrow(() -> new java.util.NoSuchElementException("Agreement item not found for id: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgreementItemView> listAgreementItems(
        UUID agreementId,
        UUID agreementItemTypeId,
        PageQuery pageQuery
    ) {
        Page<AgreementItem> page = agreementItemRepository.findItemsFiltered(
            agreementId,
            agreementItemTypeId,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(page).map(this::toAgreementItemView);
    }

    @Override
    public AgreementView changeAgreementStatus(ChangeAgreementStatusCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        String normalizedAgreementNumber = normalizeRequired(command.agreementNumber(), "agreementNumber").toUpperCase();
        AgreementStatus targetStatus = command.status();
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        String tenantCode = normalizeTenantCode(command.tenantCode());
        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeRequired(command.changedBy(), "changedBy").toLowerCase();

        Agreement agreement = agreementRepository.findByAgreementNumber(normalizedAgreementNumber)
            .orElseThrow(() -> new java.util.NoSuchElementException("Agreement not found: " + normalizedAgreementNumber));

        if (!identityActorLookup.actorExists(tenantCode, changedBy)) {
            throw new IllegalArgumentException(
                "Agreement status actor not found in tenant " + tenantCode + ": " + changedBy
            );
        }

        AgreementStatus previousStatus = agreement.getStatus();
        Instant changedAt = Instant.now(clock);
        agreement.transitionTo(targetStatus, changedAt);
        Agreement saved = agreementRepository.save(agreement);
        if (previousStatus != saved.getStatus()) {
            agreementStatusChangeAuditRepository.save(
                AgreementStatusChangeAudit.create(
                    saved.getId(),
                    previousStatus,
                    saved.getStatus(),
                    tenantCode,
                    reason,
                    changedBy,
                    changedAt
                )
            );
        }
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementView getAgreement(String agreementNumber) {
        return toView(findAgreementByNumber(agreementNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgreementView> listAgreements(PageQuery pageQuery, AgreementStatus status) {
        return listAgreements(null, pageQuery, status);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgreementView> listAgreements(String tenantCode, PageQuery pageQuery, AgreementStatus status) {
        String normalizedTenantCode = normalizeOptionalTenantCode(tenantCode);
        var pageable = pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Agreement> agreements;
        if (normalizedTenantCode == null && status == null) {
            agreements = agreementRepository.findAll(pageable);
        } else if (normalizedTenantCode == null) {
            agreements = agreementRepository.findByStatus(status, pageable);
        } else if (status == null) {
            agreements = agreementRepository.findByTenantCode(normalizedTenantCode, pageable);
        } else {
            agreements = agreementRepository.findByTenantCodeAndStatus(normalizedTenantCode, status, pageable);
        }
        return PageResult.from(agreements).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgreementStatusChangeView> listStatusHistory(
        String agreementNumber,
        String tenantCode,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        Agreement agreement = findAgreementByNumber(agreementNumber);
        String normalizedTenantCode = normalizeOptionalTenantCode(tenantCode);
        String normalizedChangedBy = normalizeOptionalChangedBy(changedBy);
        Page<AgreementStatusChangeAudit> audits = agreementStatusChangeAuditRepository.findHistoryFiltered(
            agreement.getId(),
            normalizedTenantCode,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(audits).map(audit -> new AgreementStatusChangeView(
                audit.getId(),
                agreement.getAgreementNumber(),
                audit.getPreviousStatus(),
                audit.getCurrentStatus(),
                audit.getTenantCode(),
                audit.getReason(),
                audit.getChangedBy(),
                audit.getChangedAt()
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyAgreementStatusActivitySummaryView> listDailyStatusActivitySummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
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
            DailyAgreementStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyAgreementStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
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
            WeeklyAgreementStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyAgreementStatusActivityByCurrentStatusSummaryView> listDailyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
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
            DailyAgreementStatusActivityByCurrentStatusSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyAgreementStatusActivityByCurrentStatusSummaryView> listWeeklyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
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
            WeeklyAgreementStatusActivityByCurrentStatusSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyAgreementStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
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
            MonthlyAgreementStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyAgreementStatusActivityByCurrentStatusSummaryView> listMonthlyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
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
            MonthlyAgreementStatusActivityByCurrentStatusSummaryView::new
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeTenantCode(String tenantCode) {
        return normalizeRequired(tenantCode, "tenantCode").toUpperCase();
    }

    private static String normalizeOptionalTenantCode(String tenantCode) {
        if (tenantCode == null) {
            return null;
        }
        return normalizeTenantCode(tenantCode);
    }

    private static String normalizeOptionalChangedBy(String changedBy) {
        if (changedBy == null) {
            return null;
        }
        return normalizeRequired(changedBy, "changedBy").toLowerCase();
    }

    private static String normalizeOptional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank when provided");
        }
        return normalized;
    }

    private Agreement findAgreementByNumber(String agreementNumber) {
        String normalizedAgreementNumber = normalizeRequired(agreementNumber, "agreementNumber").toUpperCase();
        return agreementRepository.findByAgreementNumber(normalizedAgreementNumber)
            .orElseThrow(() -> new java.util.NoSuchElementException("Agreement not found: " + normalizedAgreementNumber));
    }

    private AgreementView toView(Agreement agreement) {
        return new AgreementView(
            agreement.getId(),
            agreement.getTenantCode(),
            agreement.getAgreementNumber(),
            agreement.getName(),
            agreement.getAgreementType(),
            agreement.getStatus(),
            agreement.getEffectiveFrom(),
            agreement.getCreatedAt(),
            agreement.getActivatedAt(),
            agreement.getTerminatedAt()
        );
    }

    private AgreementTypeView toAgreementTypeView(AgreementType type) {
        return new AgreementTypeView(
            type.getId(),
            type.getParentId(),
            type.getLeft(),
            type.getRight(),
            type.getDescription(),
            type.getComments(),
            type.getInternalIdentifier(),
            type.getExternalIdentifier(),
            type.getExternalIdSource(),
            type.getCreatedAt()
        );
    }

    private AgreementItemTypeView toAgreementItemTypeView(AgreementItemType type) {
        return new AgreementItemTypeView(
            type.getId(),
            type.getParentId(),
            type.getLeft(),
            type.getRight(),
            type.getDescription(),
            type.getComments(),
            type.getInternalIdentifier(),
            type.getExternalIdentifier(),
            type.getExternalIdSource(),
            type.getCreatedAt()
        );
    }

    private AgreementItemView toAgreementItemView(AgreementItem item) {
        return new AgreementItemView(
            item.getId(),
            item.getAgreementId(),
            item.getAgreementItemTypeId(),
            item.getAgreementItemValue(),
            item.getDescription(),
            item.getAgreementItemRuleString(),
            item.getCreatedAt()
        );
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeStatusActivityByBucket(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery,
        StatusBucketExtractor<B> bucketExtractor,
        StatusBucketSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeOptionalTenantCode(tenantCode);
        String normalizedChangedBy = normalizeOptionalChangedBy(changedBy);
        List<AgreementStatusChangeAudit> audits = agreementStatusChangeAuditRepository.findAllHistoryFiltered(
            normalizedTenantCode,
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo
        );
        TreeMap<B, StatusBucketSummary> summaries = new TreeMap<>(java.util.Comparator.reverseOrder());
        for (AgreementStatusChangeAudit audit : audits) {
            B bucket = bucketExtractor.bucket(audit);
            StatusBucketSummary summary = summaries.computeIfAbsent(bucket, ignored -> new StatusBucketSummary());
            summary.transitionCount++;
            summary.agreementIds.add(audit.getAgreementId());
        }
        List<T> rows = summaries.entrySet().stream()
            .map(entry -> summaryFactory.create(
                entry.getKey(),
                entry.getValue().transitionCount,
                entry.getValue().agreementIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeStatusActivityByBucketAndCurrentStatus(
        String tenantCode,
        AgreementStatus previousStatus,
        AgreementStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery,
        StatusBucketExtractor<B> bucketExtractor,
        StatusBucketStatusSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeOptionalTenantCode(tenantCode);
        String normalizedChangedBy = normalizeOptionalChangedBy(changedBy);
        List<AgreementStatusChangeAudit> audits = agreementStatusChangeAuditRepository.findAllHistoryFiltered(
            normalizedTenantCode,
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo
        );
        Map<StatusBucketStatusKey<B>, StatusBucketSummary> summaries = new java.util.HashMap<>();
        for (AgreementStatusChangeAudit audit : audits) {
            StatusBucketStatusKey<B> key = new StatusBucketStatusKey<>(bucketExtractor.bucket(audit), audit.getCurrentStatus());
            StatusBucketSummary summary = summaries.computeIfAbsent(key, ignored -> new StatusBucketSummary());
            summary.transitionCount++;
            summary.agreementIds.add(audit.getAgreementId());
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
                entry.getKey().bucket(),
                entry.getKey().currentStatus(),
                entry.getValue().transitionCount,
                entry.getValue().agreementIds.size()
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
    private interface StatusBucketExtractor<B> {
        B bucket(AgreementStatusChangeAudit audit);
    }

    @FunctionalInterface
    private interface StatusBucketSummaryFactory<B, T> {
        T create(B bucket, long transitionCount, long agreementCount);
    }

    @FunctionalInterface
    private interface StatusBucketStatusSummaryFactory<B, T> {
        T create(B bucket, AgreementStatus currentStatus, long transitionCount, long agreementCount);
    }

    private record StatusBucketStatusKey<B extends Comparable<? super B>>(B bucket, AgreementStatus currentStatus) {}

    private static final class StatusBucketSummary {
        private long transitionCount;
        private final Set<UUID> agreementIds = new HashSet<>();
    }
}
