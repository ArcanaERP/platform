package com.arcanaerp.platform.inventory.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.AdjustInventoryCommand;
import com.arcanaerp.platform.inventory.DailyInventoryAdjustmentActivityByAdjustedBySummaryView;
import com.arcanaerp.platform.inventory.DailyInventoryAdjustmentActivityByLocationSummaryView;
import com.arcanaerp.platform.inventory.DailyInventoryAdjustmentActivitySummaryView;
import com.arcanaerp.platform.inventory.DailyInventoryTransferActivityByReferenceSummaryView;
import com.arcanaerp.platform.inventory.DuplicateTransferReversalException;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryAdjustmentView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import com.arcanaerp.platform.inventory.DailyInventoryTransferActivitySummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryTransferActivityByReferenceSummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryTransferActivitySummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryAdjustmentActivityByAdjustedBySummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryAdjustmentActivityByLocationSummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryAdjustmentActivitySummaryView;
import com.arcanaerp.platform.inventory.ReverseInventoryTransferCommand;
import com.arcanaerp.platform.inventory.ReversalIdempotencyPayloadConflictException;
import com.arcanaerp.platform.inventory.ReversalIdempotencyRaceConflictException;
import com.arcanaerp.platform.inventory.InventoryTransferView;
import com.arcanaerp.platform.inventory.TransferInventoryCommand;
import com.arcanaerp.platform.inventory.WeeklyInventoryTransferActivityByReferenceSummaryView;
import com.arcanaerp.platform.inventory.WeeklyInventoryTransferActivitySummaryView;
import com.arcanaerp.platform.inventory.WeeklyInventoryAdjustmentActivityByAdjustedBySummaryView;
import com.arcanaerp.platform.inventory.WeeklyInventoryAdjustmentActivityByLocationSummaryView;
import com.arcanaerp.platform.inventory.WeeklyInventoryAdjustmentActivitySummaryView;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class InventoryAvailabilityService implements InventoryAvailability {

    private static final String DEFAULT_LOCATION_CODE = "MAIN";
    private static final String TRANSFER_REVERSAL_REFERENCE_TYPE = "TRANSFER_REVERSAL";
    private static final UUID PENDING_REVERSAL_TRANSFER_ID = new UUID(0L, 0L);

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final InventoryTransferReversalIdempotencyRepository reversalIdempotencyRepository;
    private final InventoryReversalIdempotencyProperties reversalIdempotencyProperties;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public InventoryItemView inventoryForSku(String sku, String locationCode) {
        InventoryItem item = findInventoryItem(sku, locationCode);

        return new InventoryItemView(
            item.getId(),
            item.getSku(),
            item.getLocationCode(),
            item.getOnHandQuantity(),
            item.getUpdatedAt()
        );
    }

    @Override
    public InventoryAdjustmentView adjustInventory(AdjustInventoryCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        String normalizedSku = normalizeRequired(command.sku(), "sku").toUpperCase();
        String normalizedLocationCode = normalizeLocationCode(command.locationCode());
        BigDecimal quantityDelta = normalizeQuantityDelta(command.quantityDelta());
        String reason = normalizeRequired(command.reason(), "reason");
        String adjustedBy = normalizeRequired(command.adjustedBy(), "adjustedBy").toLowerCase();
        ensureLocationExists(normalizedLocationCode);

        InventoryItem item = findInventoryItem(normalizedSku, normalizedLocationCode);

        BigDecimal previousOnHand = item.getOnHandQuantity();
        Instant adjustedAt = Instant.now(clock);
        item.applyAdjustment(quantityDelta, adjustedAt);
        InventoryItem saved = inventoryItemRepository.save(item);

        InventoryAdjustment adjustment = inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                saved.getId(),
                saved.getSku(),
                saved.getLocationCode(),
                null,
                previousOnHand,
                quantityDelta,
                saved.getOnHandQuantity(),
                reason,
                adjustedBy,
                null,
                null,
                adjustedAt
            )
        );

        return new InventoryAdjustmentView(
            adjustment.getId(),
            adjustment.getSku(),
            adjustment.getLocationCode(),
            adjustment.getPreviousOnHandQuantity(),
            adjustment.getQuantityDelta(),
            adjustment.getCurrentOnHandQuantity(),
            adjustment.getReason(),
            adjustment.getAdjustedBy(),
            adjustment.getAdjustedAt()
        );
    }

    @Override
    public InventoryTransferView transferInventory(TransferInventoryCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }

        String normalizedSku = normalizeRequired(command.sku(), "sku").toUpperCase();
        String sourceLocationCode = normalizeRequired(command.sourceLocationCode(), "sourceLocationCode").toUpperCase();
        String destinationLocationCode = normalizeRequired(command.destinationLocationCode(), "destinationLocationCode").toUpperCase();
        if (sourceLocationCode.equals(destinationLocationCode)) {
            throw new IllegalArgumentException("sourceLocationCode and destinationLocationCode must be different");
        }
        BigDecimal quantity = normalizePositiveQuantity(command.quantity());
        String reason = normalizeRequired(command.reason(), "reason");
        String adjustedBy = normalizeRequired(command.adjustedBy(), "adjustedBy").toLowerCase();
        String referenceType = normalizeOptionalReferenceType(command.referenceType());
        String referenceId = normalizeOptionalReferenceId(command.referenceId());
        validateReferencePair(referenceType, referenceId);

        ensureLocationExists(sourceLocationCode);
        ensureLocationExists(destinationLocationCode);

        InventoryItem sourceItem = findInventoryItem(normalizedSku, sourceLocationCode);
        Instant adjustedAt = Instant.now(clock);
        InventoryItem destinationItem = inventoryItemRepository.findBySkuAndLocationCode(normalizedSku, destinationLocationCode)
            .orElseGet(() -> InventoryItem.create(normalizedSku, destinationLocationCode, BigDecimal.ZERO, adjustedAt));

        BigDecimal sourcePreviousOnHand = sourceItem.getOnHandQuantity();
        BigDecimal destinationPreviousOnHand = destinationItem.getOnHandQuantity();
        sourceItem.applyAdjustment(quantity.negate(), adjustedAt);
        destinationItem.applyAdjustment(quantity, adjustedAt);

        InventoryItem savedSource = inventoryItemRepository.save(sourceItem);
        InventoryItem savedDestination = inventoryItemRepository.save(destinationItem);

        UUID transferId = UUID.randomUUID();
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                savedSource.getId(),
                savedSource.getSku(),
                savedSource.getLocationCode(),
                transferId,
                sourcePreviousOnHand,
                quantity.negate(),
                savedSource.getOnHandQuantity(),
                reason,
                adjustedBy,
                referenceType,
                referenceId,
                adjustedAt
            )
        );
        inventoryAdjustmentRepository.save(
            InventoryAdjustment.create(
                savedDestination.getId(),
                savedDestination.getSku(),
                savedDestination.getLocationCode(),
                transferId,
                destinationPreviousOnHand,
                quantity,
                savedDestination.getOnHandQuantity(),
                reason,
                adjustedBy,
                referenceType,
                referenceId,
                adjustedAt
            )
        );

        return new InventoryTransferView(
            transferId,
            normalizedSku,
            sourceLocationCode,
            destinationLocationCode,
            quantity,
            savedSource.getOnHandQuantity(),
            savedDestination.getOnHandQuantity(),
            reason,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryTransferView transferById(UUID transferId) {
        if (transferId == null) {
            throw new IllegalArgumentException("transferId is required");
        }

        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository.findByTransferIdOrderByAdjustedAtAsc(transferId);
        if (adjustments.isEmpty()) {
            throw new NoSuchElementException("Inventory transfer not found: " + transferId);
        }

        InventoryAdjustment source = null;
        InventoryAdjustment destination = null;
        for (InventoryAdjustment adjustment : adjustments) {
            if (adjustment.getQuantityDelta().signum() < 0) {
                if (source != null) {
                    throw new IllegalStateException("Inventory transfer has multiple source movements for transferId: " + transferId);
                }
                source = adjustment;
                continue;
            }
            if (adjustment.getQuantityDelta().signum() > 0) {
                if (destination != null) {
                    throw new IllegalStateException("Inventory transfer has multiple destination movements for transferId: " + transferId);
                }
                destination = adjustment;
            }
        }

        if (source == null || destination == null) {
            throw new IllegalStateException("Inventory transfer data invalid for transferId: " + transferId);
        }
        if (!source.getSku().equals(destination.getSku())) {
            throw new IllegalStateException("Inventory transfer locations must share the same SKU for transferId: " + transferId);
        }

        return new InventoryTransferView(
            transferId,
            source.getSku(),
            source.getLocationCode(),
            destination.getLocationCode(),
            source.getQuantityDelta().abs(),
            source.getCurrentOnHandQuantity(),
            destination.getCurrentOnHandQuantity(),
            source.getReason(),
            source.getAdjustedBy(),
            source.getReferenceType(),
            source.getReferenceId(),
            source.getAdjustedAt()
        );
    }

    @Override
    public InventoryTransferView reverseTransfer(ReverseInventoryTransferCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        if (command.transferId() == null) {
            throw new IllegalArgumentException("transferId is required");
        }

        String reason = normalizeRequired(command.reason(), "reason");
        String adjustedBy = normalizeRequired(command.adjustedBy(), "adjustedBy").toLowerCase();
        String idempotencyKey = normalizeOptionalIdempotencyKey(command.idempotencyKey());
        if (idempotencyKey != null) {
            String requestFingerprint = fingerprintReversalRequest(reason, adjustedBy);
            return reversalIdempotencyRepository.findByTransferIdAndIdempotencyKey(command.transferId(), idempotencyKey)
                .map(existingIdempotency -> handleExistingIdempotencyClaim(
                        existingIdempotency,
                        command.transferId(),
                        reason,
                        adjustedBy,
                        idempotencyKey,
                        requestFingerprint
                    ))
                .orElseGet(() ->
                    createIdempotentReversal(command.transferId(), reason, adjustedBy, idempotencyKey, requestFingerprint, false)
                );
        }

        return createReversal(command.transferId(), reason, adjustedBy);
    }

    private InventoryTransferView createIdempotentReversal(
        UUID originalTransferId,
        String reason,
        String adjustedBy,
        String idempotencyKey,
        String requestFingerprint,
        boolean recoveringFromStaleClaim
    ) {
        try {
            InventoryTransferReversalIdempotency idempotencyRecord = reversalIdempotencyRepository.saveAndFlush(
                InventoryTransferReversalIdempotency.create(
                    originalTransferId,
                    idempotencyKey,
                    requestFingerprint,
                    PENDING_REVERSAL_TRANSFER_ID,
                    Instant.now(clock)
                )
            );

            InventoryTransferView reversal = createReversalWithStaleRecovery(originalTransferId, reason, adjustedBy, recoveringFromStaleClaim);
            idempotencyRecord.updateReversalTransferId(reversal.transferId());
            reversalIdempotencyRepository.save(idempotencyRecord);
            return reversal;
        } catch (DataIntegrityViolationException exception) {
            throw new ReversalIdempotencyRaceConflictException(
                "Idempotency-Key is already being processed for transferId: " + originalTransferId
            );
        }
    }

    private InventoryTransferView handleExistingIdempotencyClaim(
        InventoryTransferReversalIdempotency existingIdempotency,
        UUID transferId,
        String reason,
        String adjustedBy,
        String idempotencyKey,
        String requestFingerprint
    ) {
        if (!existingIdempotency.getRequestFingerprint().equals(requestFingerprint)) {
            throw new ReversalIdempotencyPayloadConflictException(
                "Idempotency-Key already used with different reversal payload for transferId: " + transferId
            );
        }
        if (!existingIdempotency.getReversalTransferId().equals(PENDING_REVERSAL_TRANSFER_ID)) {
            return transferById(existingIdempotency.getReversalTransferId());
        }
        if (!isStalePendingClaim(existingIdempotency, Instant.now(clock))) {
            throw new ReversalIdempotencyRaceConflictException(
                "Idempotency-Key is already being processed for transferId: " + transferId
            );
        }
        if (!releasePendingClaim(existingIdempotency)) {
            throw new ReversalIdempotencyRaceConflictException(
                "Idempotency-Key is already being processed for transferId: " + transferId
            );
        }
        return createIdempotentReversal(transferId, reason, adjustedBy, idempotencyKey, requestFingerprint, true);
    }

    private InventoryTransferView createReversalWithStaleRecovery(
        UUID originalTransferId,
        String reason,
        String adjustedBy,
        boolean recoveringFromStaleClaim
    ) {
        try {
            return createReversal(originalTransferId, reason, adjustedBy);
        } catch (DuplicateTransferReversalException duplicateTransferReversalException) {
            if (!recoveringFromStaleClaim) {
                throw duplicateTransferReversalException;
            }
            return findLinkedReversal(originalTransferId).orElseThrow(() -> duplicateTransferReversalException);
        }
    }

    private java.util.Optional<InventoryTransferView> findLinkedReversal(UUID originalTransferId) {
        PageResult<InventoryTransferView> reversals = listReversals(originalTransferId, PageQuery.of(0, 1));
        if (reversals.totalItems() == 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(reversals.items().get(0));
    }

    private InventoryTransferView createReversal(
        UUID originalTransferId,
        String reason,
        String adjustedBy
    ) {
        InventoryTransferView original = transferById(originalTransferId);
        PageResult<InventoryTransferView> existingReversals = listTransfers(
            original.sku(),
            null,
            null,
            null,
            TRANSFER_REVERSAL_REFERENCE_TYPE,
            original.transferId().toString(),
            null,
            null,
            PageQuery.of(0, 1)
        );
        if (existingReversals.totalItems() > 0) {
            throw new DuplicateTransferReversalException("Inventory transfer already reversed: " + original.transferId());
        }
        InventoryTransferView reversal = transferInventory(
            new TransferInventoryCommand(
                original.sku(),
                original.destinationLocationCode(),
                original.sourceLocationCode(),
                original.quantity(),
                reason,
                adjustedBy,
                TRANSFER_REVERSAL_REFERENCE_TYPE,
                original.transferId().toString()
            )
        );
        return reversal;
    }

    private boolean isStalePendingClaim(InventoryTransferReversalIdempotency existingIdempotency, Instant now) {
        Instant staleBefore = now.minus(reversalIdempotencyProperties.getPendingClaimTtl());
        return !existingIdempotency.getCreatedAt().isAfter(staleBefore);
    }

    private boolean releasePendingClaim(InventoryTransferReversalIdempotency existingIdempotency) {
        long deleted = reversalIdempotencyRepository.deleteByIdAndReversalTransferId(
            existingIdempotency.getId(),
            PENDING_REVERSAL_TRANSFER_ID
        );
        if (deleted == 1) {
            reversalIdempotencyRepository.flush();
        }
        return deleted == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryTransferView> listReversals(UUID transferId, PageQuery pageQuery) {
        if (transferId == null) {
            throw new IllegalArgumentException("transferId is required");
        }

        InventoryTransferView original = transferById(transferId);
        return listTransfers(
            original.sku(),
            null,
            null,
            null,
            TRANSFER_REVERSAL_REFERENCE_TYPE,
            transferId.toString(),
            null,
            null,
            pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryTransferView> listTransfers(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        ensureSkuExists(normalizedSku);
        String normalizedSourceLocationCode = normalizeOptionalLocationCodeFilter(sourceLocationCode, "sourceLocationCode");
        String normalizedDestinationLocationCode = normalizeOptionalLocationCodeFilter(
            destinationLocationCode,
            "destinationLocationCode"
        );
        String normalizedAdjustedBy = adjustedBy == null ? null : normalizeRequired(adjustedBy, "adjustedBy").toLowerCase();
        String normalizedReferenceType = normalizeOptionalReferenceType(referenceType);
        String normalizedReferenceId = normalizeOptionalReferenceId(referenceId);

        Page<InventoryAdjustmentRepository.TransferHistoryProjection> transfers = inventoryAdjustmentRepository.findTransferHistoryFiltered(
            normalizedSku,
            normalizedSourceLocationCode,
            normalizedDestinationLocationCode,
            normalizedAdjustedBy,
            normalizedReferenceType,
            normalizedReferenceId,
            adjustedAtFrom,
            adjustedAtTo,
            PageRequest.of(pageQuery.page(), pageQuery.size())
        );

        return PageResult.from(transfers).map(transfer -> new InventoryTransferView(
                transfer.getTransferId(),
                transfer.getSku(),
                transfer.getSourceLocationCode(),
                transfer.getDestinationLocationCode(),
                transfer.getSourceQuantityDelta().abs(),
                transfer.getSourceOnHandQuantity(),
                transfer.getDestinationOnHandQuantity(),
                transfer.getReason(),
                transfer.getAdjustedBy(),
                transfer.getReferenceType(),
                transfer.getReferenceId(),
                transfer.getTransferredAt()
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InventoryAdjustmentView> listAdjustments(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        InventoryItem item = findInventoryItem(sku, locationCode);
        String normalizedAdjustedBy = adjustedBy == null ? null : normalizeRequired(adjustedBy, "adjustedBy").toLowerCase();
        PageRequest pageRequest = PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.DESC, "adjustedAt"));

        Page<InventoryAdjustment> adjustments = inventoryAdjustmentRepository.findHistoryFiltered(
            item.getId(),
            normalizedAdjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageRequest
        );

        return PageResult.from(adjustments).map(adjustment -> new InventoryAdjustmentView(
                adjustment.getId(),
                adjustment.getSku(),
                adjustment.getLocationCode(),
                adjustment.getPreviousOnHandQuantity(),
                adjustment.getQuantityDelta(),
                adjustment.getCurrentOnHandQuantity(),
                adjustment.getReason(),
                adjustment.getAdjustedBy(),
                adjustment.getAdjustedAt()
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyInventoryTransferActivitySummaryView> listDailyTransferActivitySummaries(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTransferActivityByBucket(
            sku,
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            transfer -> transfer.getTransferredAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyInventoryTransferActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyInventoryTransferActivitySummaryView> listWeeklyTransferActivitySummaries(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTransferActivityByBucket(
            sku,
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            transfer -> transfer.getTransferredAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyInventoryTransferActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyInventoryTransferActivityByReferenceSummaryView> listDailyTransferActivityByReferenceSummaries(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTransferActivityByBucketAndReference(
            sku,
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            transfer -> transfer.getTransferredAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyInventoryTransferActivityByReferenceSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyInventoryTransferActivityByReferenceSummaryView> listWeeklyTransferActivityByReferenceSummaries(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTransferActivityByBucketAndReference(
            sku,
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            transfer -> transfer.getTransferredAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyInventoryTransferActivityByReferenceSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyInventoryTransferActivitySummaryView> listMonthlyTransferActivitySummaries(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTransferActivityByBucket(
            sku,
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            transfer -> YearMonth.from(transfer.getTransferredAt().atOffset(ZoneOffset.UTC)),
            MonthlyInventoryTransferActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyInventoryTransferActivityByReferenceSummaryView> listMonthlyTransferActivityByReferenceSummaries(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTransferActivityByBucketAndReference(
            sku,
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            transfer -> YearMonth.from(transfer.getTransferredAt().atOffset(ZoneOffset.UTC)),
            MonthlyInventoryTransferActivityByReferenceSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyInventoryAdjustmentActivitySummaryView> listDailyAdjustmentActivitySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAdjustmentActivityByBucket(
            sku,
            locationCode,
            adjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            adjustment -> adjustment.getAdjustedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyInventoryAdjustmentActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyInventoryAdjustmentActivitySummaryView> listWeeklyAdjustmentActivitySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAdjustmentActivityByBucket(
            sku,
            locationCode,
            adjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            adjustment -> adjustment.getAdjustedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyInventoryAdjustmentActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyInventoryAdjustmentActivityByLocationSummaryView> listDailyAdjustmentActivityByLocationSummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAdjustmentActivityByBucketAndLocation(
            sku,
            locationCode,
            adjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            adjustment -> adjustment.getAdjustedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyInventoryAdjustmentActivityByLocationSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyInventoryAdjustmentActivityByLocationSummaryView> listWeeklyAdjustmentActivityByLocationSummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAdjustmentActivityByBucketAndLocation(
            sku,
            locationCode,
            adjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            adjustment -> adjustment.getAdjustedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyInventoryAdjustmentActivityByLocationSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyInventoryAdjustmentActivityByAdjustedBySummaryView> listDailyAdjustmentActivityByAdjustedBySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAdjustmentActivityByBucketAndAdjustedBy(
            sku,
            locationCode,
            adjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            adjustment -> adjustment.getAdjustedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyInventoryAdjustmentActivityByAdjustedBySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyInventoryAdjustmentActivityByAdjustedBySummaryView> listWeeklyAdjustmentActivityByAdjustedBySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAdjustmentActivityByBucketAndAdjustedBy(
            sku,
            locationCode,
            adjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            adjustment -> adjustment.getAdjustedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyInventoryAdjustmentActivityByAdjustedBySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyInventoryAdjustmentActivitySummaryView> listMonthlyAdjustmentActivitySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAdjustmentActivityByBucket(
            sku,
            locationCode,
            adjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            adjustment -> YearMonth.from(adjustment.getAdjustedAt().atOffset(ZoneOffset.UTC)),
            MonthlyInventoryAdjustmentActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyInventoryAdjustmentActivityByLocationSummaryView> listMonthlyAdjustmentActivityByLocationSummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAdjustmentActivityByBucketAndLocation(
            sku,
            locationCode,
            adjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            adjustment -> YearMonth.from(adjustment.getAdjustedAt().atOffset(ZoneOffset.UTC)),
            MonthlyInventoryAdjustmentActivityByLocationSummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyInventoryAdjustmentActivityByAdjustedBySummaryView> listMonthlyAdjustmentActivityByAdjustedBySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeAdjustmentActivityByBucketAndAdjustedBy(
            sku,
            locationCode,
            adjustedBy,
            adjustedAtFrom,
            adjustedAtTo,
            pageQuery,
            adjustment -> YearMonth.from(adjustment.getAdjustedAt().atOffset(ZoneOffset.UTC)),
            MonthlyInventoryAdjustmentActivityByAdjustedBySummaryView::new
        );
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeAdjustmentActivityByBucket(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery,
        AdjustmentBucketExtractor<B> bucketExtractor,
        AdjustmentBucketSummaryFactory<B, T> summaryFactory
    ) {
        InventoryItem item = findInventoryItem(sku, locationCode);
        String normalizedAdjustedBy = adjustedBy == null ? null : normalizeRequired(adjustedBy, "adjustedBy").toLowerCase();
        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository.findHistoryRowsFiltered(
            item.getId(),
            normalizedAdjustedBy,
            adjustedAtFrom,
            adjustedAtTo
        );
        TreeMap<B, AdjustmentBucketSummary> summaries = new TreeMap<>(java.util.Comparator.reverseOrder());
        for (InventoryAdjustment adjustment : adjustments) {
            AdjustmentBucketSummary summary = summaries.computeIfAbsent(
                bucketExtractor.bucket(adjustment),
                ignored -> new AdjustmentBucketSummary()
            );
            summary.adjustmentCount++;
            summary.netQuantityDelta = summary.netQuantityDelta.add(adjustment.getQuantityDelta());
        }
        List<T> rows = summaries.entrySet().stream()
            .map(entry -> summaryFactory.create(
                item.getSku(),
                item.getLocationCode(),
                entry.getKey(),
                entry.getValue().adjustmentCount,
                entry.getValue().netQuantityDelta
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeTransferActivityByBucket(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery,
        TransferBucketExtractor<B> bucketExtractor,
        TransferBucketSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        ensureSkuExists(normalizedSku);
        String normalizedSourceLocationCode = normalizeOptionalLocationCodeFilter(sourceLocationCode, "sourceLocationCode");
        String normalizedDestinationLocationCode = normalizeOptionalLocationCodeFilter(
            destinationLocationCode,
            "destinationLocationCode"
        );
        String normalizedAdjustedBy = adjustedBy == null ? null : normalizeRequired(adjustedBy, "adjustedBy").toLowerCase();
        String normalizedReferenceType = normalizeOptionalReferenceType(referenceType);
        String normalizedReferenceId = normalizeOptionalReferenceId(referenceId);

        List<InventoryAdjustmentRepository.TransferHistoryProjection> transfers =
            inventoryAdjustmentRepository.findTransferHistoryRowsFiltered(
                normalizedSku,
                normalizedSourceLocationCode,
                normalizedDestinationLocationCode,
                normalizedAdjustedBy,
                normalizedReferenceType,
                normalizedReferenceId,
                adjustedAtFrom,
                adjustedAtTo
            );
        Map<TransferBucketKey<B>, TransferBucketSummary> summaries = new java.util.HashMap<>();
        for (InventoryAdjustmentRepository.TransferHistoryProjection transfer : transfers) {
            TransferBucketKey<B> key = new TransferBucketKey<>(
                bucketExtractor.bucket(transfer),
                transfer.getSourceLocationCode(),
                transfer.getDestinationLocationCode(),
                transfer.getAdjustedBy()
            );
            TransferBucketSummary summary = summaries.computeIfAbsent(key, ignored -> new TransferBucketSummary());
            summary.transferCount++;
            summary.totalQuantity = summary.totalQuantity.add(transfer.getSourceQuantityDelta().abs());
        }
        List<T> rows = summaries.entrySet().stream()
            .sorted((left, right) -> {
                int bucketComparison = right.getKey().bucket().compareTo(left.getKey().bucket());
                if (bucketComparison != 0) {
                    return bucketComparison;
                }
                int sourceComparison = left.getKey().sourceLocationCode().compareTo(right.getKey().sourceLocationCode());
                if (sourceComparison != 0) {
                    return sourceComparison;
                }
                int destinationComparison = left.getKey().destinationLocationCode()
                    .compareTo(right.getKey().destinationLocationCode());
                if (destinationComparison != 0) {
                    return destinationComparison;
                }
                return left.getKey().adjustedBy().compareTo(right.getKey().adjustedBy());
            })
            .map(entry -> summaryFactory.create(
                normalizedSku,
                entry.getKey().bucket(),
                entry.getKey().sourceLocationCode(),
                entry.getKey().destinationLocationCode(),
                entry.getKey().adjustedBy(),
                entry.getValue().transferCount,
                entry.getValue().totalQuantity
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeTransferActivityByBucketAndReference(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery,
        TransferBucketExtractor<B> bucketExtractor,
        TransferBucketReferenceSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        ensureSkuExists(normalizedSku);
        String normalizedSourceLocationCode = normalizeOptionalLocationCodeFilter(sourceLocationCode, "sourceLocationCode");
        String normalizedDestinationLocationCode = normalizeOptionalLocationCodeFilter(
            destinationLocationCode,
            "destinationLocationCode"
        );
        String normalizedAdjustedBy = adjustedBy == null ? null : normalizeRequired(adjustedBy, "adjustedBy").toLowerCase();
        String normalizedReferenceType = normalizeOptionalReferenceType(referenceType);
        String normalizedReferenceId = normalizeOptionalReferenceId(referenceId);

        List<InventoryAdjustmentRepository.TransferHistoryProjection> transfers =
            inventoryAdjustmentRepository.findTransferHistoryRowsFiltered(
                normalizedSku,
                normalizedSourceLocationCode,
                normalizedDestinationLocationCode,
                normalizedAdjustedBy,
                normalizedReferenceType,
                normalizedReferenceId,
                adjustedAtFrom,
                adjustedAtTo
            );
        Map<TransferBucketReferenceKey<B>, TransferBucketSummary> summaries = new java.util.HashMap<>();
        for (InventoryAdjustmentRepository.TransferHistoryProjection transfer : transfers) {
            TransferBucketReferenceKey<B> key = new TransferBucketReferenceKey<>(
                bucketExtractor.bucket(transfer),
                transfer.getReferenceType(),
                transfer.getReferenceId()
            );
            TransferBucketSummary summary = summaries.computeIfAbsent(key, ignored -> new TransferBucketSummary());
            summary.transferCount++;
            summary.totalQuantity = summary.totalQuantity.add(transfer.getSourceQuantityDelta().abs());
        }
        List<T> rows = summaries.entrySet().stream()
            .sorted((left, right) -> {
                int bucketComparison = right.getKey().bucket().compareTo(left.getKey().bucket());
                if (bucketComparison != 0) {
                    return bucketComparison;
                }
                int referenceTypeComparison = compareNullable(left.getKey().referenceType(), right.getKey().referenceType());
                if (referenceTypeComparison != 0) {
                    return referenceTypeComparison;
                }
                return compareNullable(left.getKey().referenceId(), right.getKey().referenceId());
            })
            .map(entry -> summaryFactory.create(
                normalizedSku,
                entry.getKey().bucket(),
                entry.getKey().referenceType(),
                entry.getKey().referenceId(),
                entry.getValue().transferCount,
                entry.getValue().totalQuantity
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private static int compareNullable(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeAdjustmentActivityByBucketAndLocation(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery,
        AdjustmentBucketExtractor<B> bucketExtractor,
        AdjustmentBucketLocationSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        ensureSkuExists(normalizedSku);
        String normalizedLocationCode = normalizeOptionalLocationCodeFilter(locationCode, "locationCode");
        String normalizedAdjustedBy = adjustedBy == null ? null : normalizeRequired(adjustedBy, "adjustedBy").toLowerCase();
        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository.findSkuHistoryRowsFiltered(
            normalizedSku,
            normalizedLocationCode,
            normalizedAdjustedBy,
            adjustedAtFrom,
            adjustedAtTo
        );
        Map<AdjustmentBucketLocationKey<B>, AdjustmentBucketSummary> summaries = new java.util.HashMap<>();
        for (InventoryAdjustment adjustment : adjustments) {
            AdjustmentBucketLocationKey<B> key = new AdjustmentBucketLocationKey<>(
                bucketExtractor.bucket(adjustment),
                adjustment.getLocationCode()
            );
            AdjustmentBucketSummary summary = summaries.computeIfAbsent(key, ignored -> new AdjustmentBucketSummary());
            summary.adjustmentCount++;
            summary.netQuantityDelta = summary.netQuantityDelta.add(adjustment.getQuantityDelta());
        }
        List<T> rows = summaries.entrySet().stream()
            .sorted((left, right) -> {
                int bucketComparison = right.getKey().bucket().compareTo(left.getKey().bucket());
                if (bucketComparison != 0) {
                    return bucketComparison;
                }
                return left.getKey().locationCode().compareTo(right.getKey().locationCode());
            })
            .map(entry -> summaryFactory.create(
                normalizedSku,
                entry.getKey().bucket(),
                entry.getKey().locationCode(),
                entry.getValue().adjustmentCount,
                entry.getValue().netQuantityDelta
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeAdjustmentActivityByBucketAndAdjustedBy(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery,
        AdjustmentBucketExtractor<B> bucketExtractor,
        AdjustmentBucketAdjustedBySummaryFactory<B, T> summaryFactory
    ) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        ensureSkuExists(normalizedSku);
        String normalizedLocationCode = normalizeOptionalLocationCodeFilter(locationCode, "locationCode");
        String normalizedAdjustedBy = adjustedBy == null ? null : normalizeRequired(adjustedBy, "adjustedBy").toLowerCase();
        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository.findSkuHistoryRowsFiltered(
            normalizedSku,
            normalizedLocationCode,
            normalizedAdjustedBy,
            adjustedAtFrom,
            adjustedAtTo
        );
        Map<AdjustmentBucketAdjustedByKey<B>, AdjustmentBucketSummary> summaries = new java.util.HashMap<>();
        for (InventoryAdjustment adjustment : adjustments) {
            AdjustmentBucketAdjustedByKey<B> key = new AdjustmentBucketAdjustedByKey<>(
                bucketExtractor.bucket(adjustment),
                adjustment.getAdjustedBy()
            );
            AdjustmentBucketSummary summary = summaries.computeIfAbsent(key, ignored -> new AdjustmentBucketSummary());
            summary.adjustmentCount++;
            summary.netQuantityDelta = summary.netQuantityDelta.add(adjustment.getQuantityDelta());
        }
        List<T> rows = summaries.entrySet().stream()
            .sorted((left, right) -> {
                int bucketComparison = right.getKey().bucket().compareTo(left.getKey().bucket());
                if (bucketComparison != 0) {
                    return bucketComparison;
                }
                return left.getKey().adjustedBy().compareTo(right.getKey().adjustedBy());
            })
            .map(entry -> summaryFactory.create(
                normalizedSku,
                entry.getKey().bucket(),
                entry.getKey().adjustedBy(),
                entry.getValue().adjustmentCount,
                entry.getValue().netQuantityDelta
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
    private interface AdjustmentBucketExtractor<B> {
        B bucket(InventoryAdjustment adjustment);
    }

    @FunctionalInterface
    private interface TransferBucketExtractor<B> {
        B bucket(InventoryAdjustmentRepository.TransferHistoryProjection transfer);
    }

    @FunctionalInterface
    private interface AdjustmentBucketSummaryFactory<B, T> {
        T create(String sku, String locationCode, B bucket, long adjustmentCount, BigDecimal netQuantityDelta);
    }

    @FunctionalInterface
    private interface AdjustmentBucketLocationSummaryFactory<B, T> {
        T create(String sku, B bucket, String locationCode, long adjustmentCount, BigDecimal netQuantityDelta);
    }

    @FunctionalInterface
    private interface AdjustmentBucketAdjustedBySummaryFactory<B, T> {
        T create(String sku, B bucket, String adjustedBy, long adjustmentCount, BigDecimal netQuantityDelta);
    }

    @FunctionalInterface
    private interface TransferBucketSummaryFactory<B, T> {
        T create(
            String sku,
            B bucket,
            String sourceLocationCode,
            String destinationLocationCode,
            String adjustedBy,
            long transferCount,
            BigDecimal totalQuantity
        );
    }

    @FunctionalInterface
    private interface TransferBucketReferenceSummaryFactory<B, T> {
        T create(
            String sku,
            B bucket,
            String referenceType,
            String referenceId,
            long transferCount,
            BigDecimal totalQuantity
        );
    }

    private record AdjustmentBucketLocationKey<B extends Comparable<? super B>>(B bucket, String locationCode) {
    }

    private record AdjustmentBucketAdjustedByKey<B extends Comparable<? super B>>(B bucket, String adjustedBy) {
    }

    private record TransferBucketKey<B extends Comparable<? super B>>(
        B bucket,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy
    ) {
    }

    private record TransferBucketReferenceKey<B extends Comparable<? super B>>(
        B bucket,
        String referenceType,
        String referenceId
    ) {
    }

    private static final class AdjustmentBucketSummary {
        private long adjustmentCount;
        private BigDecimal netQuantityDelta = BigDecimal.ZERO;
    }

    private static final class TransferBucketSummary {
        private long transferCount;
        private BigDecimal totalQuantity = BigDecimal.ZERO;
    }

    private static BigDecimal normalizeQuantityDelta(BigDecimal quantityDelta) {
        if (quantityDelta == null) {
            throw new IllegalArgumentException("quantityDelta is required");
        }
        if (quantityDelta.signum() == 0) {
            throw new IllegalArgumentException("quantityDelta must not be zero");
        }
        return quantityDelta;
    }

    private static BigDecimal normalizePositiveQuantity(BigDecimal quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("quantity is required");
        }
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        return quantity;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalReferenceType(String referenceType) {
        if (referenceType == null) {
            return null;
        }
        if (referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType is required");
        }
        return referenceType.trim().toUpperCase();
    }

    private static String normalizeOptionalReferenceId(String referenceId) {
        if (referenceId == null) {
            return null;
        }
        if (referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId is required");
        }
        return referenceId.trim();
    }

    private static String normalizeOptionalIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header must not be blank");
        }
        return idempotencyKey.trim();
    }

    private static String fingerprintReversalRequest(String reason, String adjustedBy) {
        String canonicalRequest = reason + "\n" + adjustedBy;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }

    private static void validateReferencePair(String referenceType, String referenceId) {
        if ((referenceType == null) == (referenceId == null)) {
            return;
        }
        throw new IllegalArgumentException("referenceType and referenceId must both be provided together");
    }

    private static String normalizeLocationCode(String locationCode) {
        if (locationCode == null) {
            return DEFAULT_LOCATION_CODE;
        }
        if (locationCode.isBlank()) {
            throw new IllegalArgumentException("locationCode is required");
        }
        return locationCode.trim().toUpperCase();
    }

    private static String normalizeOptionalLocationCodeFilter(String locationCode, String fieldName) {
        if (locationCode == null) {
            return null;
        }
        if (locationCode.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return locationCode.trim().toUpperCase();
    }

    private InventoryItem findInventoryItem(String sku, String locationCode) {
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        String normalizedLocationCode = normalizeLocationCode(locationCode);
        return inventoryItemRepository.findBySkuAndLocationCode(normalizedSku, normalizedLocationCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Inventory item not found for SKU: " + normalizedSku + " at location: " + normalizedLocationCode
            ));
    }

    private void ensureSkuExists(String sku) {
        if (!inventoryItemRepository.existsBySku(sku)) {
            throw new NoSuchElementException("Inventory item not found for SKU: " + sku);
        }
    }

    private void ensureLocationExists(String locationCode) {
        inventoryLocationRepository.findByCode(locationCode)
            .orElseGet(() -> inventoryLocationRepository.save(
                InventoryLocation.create(locationCode, locationCode, Instant.now(clock))
            ));
    }
}
