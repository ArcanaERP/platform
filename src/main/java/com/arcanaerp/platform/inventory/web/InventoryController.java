package com.arcanaerp.platform.inventory.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.inventory.AdjustInventoryCommand;
import com.arcanaerp.platform.inventory.DailyInventoryAdjustmentActivityByAdjustedBySummaryView;
import com.arcanaerp.platform.inventory.DailyInventoryAdjustmentActivityByLocationSummaryView;
import com.arcanaerp.platform.inventory.DailyInventoryAdjustmentActivitySummaryView;
import com.arcanaerp.platform.inventory.DailyInventoryPickupDropoffActivitySummaryView;
import com.arcanaerp.platform.inventory.InventoryAvailability;
import com.arcanaerp.platform.inventory.InventoryAdjustmentView;
import com.arcanaerp.platform.inventory.InventoryItemView;
import com.arcanaerp.platform.inventory.InventoryPickupDropoffTransactionView;
import com.arcanaerp.platform.inventory.MonthlyInventoryPickupDropoffActivitySummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryAdjustmentActivityByAdjustedBySummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryAdjustmentActivityByLocationSummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryAdjustmentActivitySummaryView;
import com.arcanaerp.platform.inventory.DailyInventoryTransferActivityByReferenceSummaryView;
import com.arcanaerp.platform.inventory.DailyInventoryTransferActivitySummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryTransferActivityByReferenceSummaryView;
import com.arcanaerp.platform.inventory.MonthlyInventoryTransferActivitySummaryView;
import com.arcanaerp.platform.inventory.ReverseInventoryTransferCommand;
import com.arcanaerp.platform.inventory.RecordInventoryPickupDropoffCommand;
import com.arcanaerp.platform.inventory.InventoryTransferView;
import com.arcanaerp.platform.inventory.TransferInventoryCommand;
import com.arcanaerp.platform.inventory.WeeklyInventoryTransferActivityByReferenceSummaryView;
import com.arcanaerp.platform.inventory.WeeklyInventoryTransferActivitySummaryView;
import com.arcanaerp.platform.inventory.WeeklyInventoryAdjustmentActivityByAdjustedBySummaryView;
import com.arcanaerp.platform.inventory.WeeklyInventoryAdjustmentActivityByLocationSummaryView;
import com.arcanaerp.platform.inventory.WeeklyInventoryAdjustmentActivitySummaryView;
import com.arcanaerp.platform.inventory.WeeklyInventoryPickupDropoffActivitySummaryView;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final String DEFAULT_LOCATION_CODE = "MAIN";

    private final InventoryAvailability inventoryAvailability;

    @GetMapping("/{sku}")
    public InventoryItemResponse inventoryBySku(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode
    ) {
        InventoryItemView item = inventoryAvailability.inventoryForSku(sku, normalizeOptionalLocationCode(locationCode));
        return new InventoryItemResponse(
            item.id(),
            item.sku(),
            item.locationCode(),
            item.onHandQuantity(),
            item.availableQuantity(),
            item.soldQuantity(),
            item.unitOfMeasurementCode(),
            item.classificationCode(),
            item.productInstanceCode(),
            item.externalReference(),
            item.sourceSystemCode(),
            item.ownerUserId(),
            item.ownerTenantCode(),
            item.ownerRoleCode(),
            item.updatedAt()
        );
    }

    @GetMapping("/{sku}/adjustments")
    public PageResult<InventoryAdjustmentResponse> listAdjustments(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        String normalizedAdjustedBy = normalizeOptionalAdjustedBy(adjustedBy);
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listAdjustments(
                sku,
                normalizeOptionalLocationCode(locationCode),
                normalizedAdjustedBy,
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toAdjustmentResponse);
    }

    @PostMapping("/{sku}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryAdjustmentResponse adjustInventory(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @Valid @RequestBody AdjustInventoryRequest request
    ) {
        InventoryAdjustmentView adjustment = inventoryAvailability.adjustInventory(
            new AdjustInventoryCommand(
                sku,
                normalizeOptionalLocationCode(locationCode),
                request.quantityDelta(),
                request.reason(),
                request.adjustedBy()
            )
        );
        return toAdjustmentResponse(adjustment);
    }

    @PostMapping("/{sku}/pickup-dropoffs")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryPickupDropoffTransactionResponse recordPickupDropoff(
        @PathVariable String sku,
        @Valid @RequestBody RecordInventoryPickupDropoffRequest request
    ) {
        InventoryPickupDropoffTransactionView transaction = inventoryAvailability.recordPickupDropoff(
            new RecordInventoryPickupDropoffCommand(
                sku,
                request.locationCode(),
                request.transactionTypeCode(),
                request.quantity(),
                request.reason(),
                request.handledBy(),
                request.fixedAssetCode(),
                request.facilityCode(),
                request.referenceType(),
                request.referenceId()
            )
        );
        return toPickupDropoffTransactionResponse(transaction);
    }

    @GetMapping("/{sku}/pickup-dropoffs")
    public PageResult<InventoryPickupDropoffTransactionResponse> listPickupDropoffs(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String transactionTypeCode,
        @RequestParam(required = false) String handledBy,
        @RequestParam(required = false) String fixedAssetCode,
        @RequestParam(required = false) String facilityCode,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String transactionAtFrom,
        @RequestParam(required = false) String transactionAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedTransactionAtFrom = parseOptionalInstant(transactionAtFrom, "transactionAtFrom");
        Instant parsedTransactionAtTo = parseOptionalInstant(transactionAtTo, "transactionAtTo");
        validateTransactionAtRange(parsedTransactionAtFrom, parsedTransactionAtTo);

        return inventoryAvailability.listPickupDropoffs(
                sku,
                normalizeOptionalTransferLocationCode(locationCode, "locationCode"),
                normalizeOptionalTransactionTypeCode(transactionTypeCode),
                normalizeOptionalHandledBy(handledBy),
                normalizeOptionalFixedAssetCode(fixedAssetCode),
                normalizeOptionalFacilityCode(facilityCode),
                normalizeOptionalReferenceType(referenceType),
                normalizeOptionalReferenceId(referenceId),
                parsedTransactionAtFrom,
                parsedTransactionAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toPickupDropoffTransactionResponse);
    }

    @GetMapping("/{sku}/pickup-dropoff-activity/daily-summary")
    public PageResult<DailyInventoryPickupDropoffActivitySummaryResponse> listDailyPickupDropoffActivitySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String transactionTypeCode,
        @RequestParam(required = false) String handledBy,
        @RequestParam(required = false) String fixedAssetCode,
        @RequestParam(required = false) String facilityCode,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String transactionAtFrom,
        @RequestParam(required = false) String transactionAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        PickupDropoffActivityQuery query = parsePickupDropoffActivityQuery(
            locationCode,
            transactionTypeCode,
            handledBy,
            fixedAssetCode,
            facilityCode,
            referenceType,
            referenceId,
            transactionAtFrom,
            transactionAtTo
        );

        return inventoryAvailability.listDailyPickupDropoffActivitySummaries(
                sku,
                query.locationCode(),
                query.transactionTypeCode(),
                query.handledBy(),
                query.fixedAssetCode(),
                query.facilityCode(),
                query.referenceType(),
                query.referenceId(),
                query.transactionAtFrom(),
                query.transactionAtTo(),
                PageQuery.of(page, size)
            )
            .map(this::toDailyPickupDropoffActivitySummaryResponse);
    }

    @GetMapping("/{sku}/pickup-dropoff-activity/weekly-summary")
    public PageResult<WeeklyInventoryPickupDropoffActivitySummaryResponse> listWeeklyPickupDropoffActivitySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String transactionTypeCode,
        @RequestParam(required = false) String handledBy,
        @RequestParam(required = false) String fixedAssetCode,
        @RequestParam(required = false) String facilityCode,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String transactionAtFrom,
        @RequestParam(required = false) String transactionAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        PickupDropoffActivityQuery query = parsePickupDropoffActivityQuery(
            locationCode,
            transactionTypeCode,
            handledBy,
            fixedAssetCode,
            facilityCode,
            referenceType,
            referenceId,
            transactionAtFrom,
            transactionAtTo
        );

        return inventoryAvailability.listWeeklyPickupDropoffActivitySummaries(
                sku,
                query.locationCode(),
                query.transactionTypeCode(),
                query.handledBy(),
                query.fixedAssetCode(),
                query.facilityCode(),
                query.referenceType(),
                query.referenceId(),
                query.transactionAtFrom(),
                query.transactionAtTo(),
                PageQuery.of(page, size)
            )
            .map(this::toWeeklyPickupDropoffActivitySummaryResponse);
    }

    @GetMapping("/{sku}/pickup-dropoff-activity/monthly-summary")
    public PageResult<MonthlyInventoryPickupDropoffActivitySummaryResponse> listMonthlyPickupDropoffActivitySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String transactionTypeCode,
        @RequestParam(required = false) String handledBy,
        @RequestParam(required = false) String fixedAssetCode,
        @RequestParam(required = false) String facilityCode,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String transactionAtFrom,
        @RequestParam(required = false) String transactionAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        PickupDropoffActivityQuery query = parsePickupDropoffActivityQuery(
            locationCode,
            transactionTypeCode,
            handledBy,
            fixedAssetCode,
            facilityCode,
            referenceType,
            referenceId,
            transactionAtFrom,
            transactionAtTo
        );

        return inventoryAvailability.listMonthlyPickupDropoffActivitySummaries(
                sku,
                query.locationCode(),
                query.transactionTypeCode(),
                query.handledBy(),
                query.fixedAssetCode(),
                query.facilityCode(),
                query.referenceType(),
                query.referenceId(),
                query.transactionAtFrom(),
                query.transactionAtTo(),
                PageQuery.of(page, size)
            )
            .map(this::toMonthlyPickupDropoffActivitySummaryResponse);
    }

    @GetMapping("/{sku}/adjustment-activity/daily-summary")
    public PageResult<DailyInventoryAdjustmentActivitySummaryResponse> listDailyAdjustmentActivitySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listDailyAdjustmentActivitySummaries(
                sku,
                normalizeOptionalLocationCode(locationCode),
                normalizeOptionalAdjustedBy(adjustedBy),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toDailyAdjustmentActivitySummaryResponse);
    }

    @GetMapping("/{sku}/adjustment-activity/daily-summary/by-location")
    public PageResult<DailyInventoryAdjustmentActivityByLocationSummaryResponse> listDailyAdjustmentActivityByLocationSummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listDailyAdjustmentActivityByLocationSummaries(
                sku,
                normalizeOptionalTransferLocationCode(locationCode, "locationCode"),
                normalizeOptionalAdjustedBy(adjustedBy),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toDailyAdjustmentActivityByLocationSummaryResponse);
    }

    @GetMapping("/{sku}/adjustment-activity/daily-summary/by-adjusted-by")
    public PageResult<DailyInventoryAdjustmentActivityByAdjustedBySummaryResponse> listDailyAdjustmentActivityByAdjustedBySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listDailyAdjustmentActivityByAdjustedBySummaries(
                sku,
                normalizeOptionalTransferLocationCode(locationCode, "locationCode"),
                normalizeOptionalAdjustedBy(adjustedBy),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toDailyAdjustmentActivityByAdjustedBySummaryResponse);
    }

    @GetMapping("/{sku}/adjustment-activity/weekly-summary")
    public PageResult<WeeklyInventoryAdjustmentActivitySummaryResponse> listWeeklyAdjustmentActivitySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listWeeklyAdjustmentActivitySummaries(
                sku,
                normalizeOptionalLocationCode(locationCode),
                normalizeOptionalAdjustedBy(adjustedBy),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toWeeklyAdjustmentActivitySummaryResponse);
    }

    @GetMapping("/{sku}/adjustment-activity/weekly-summary/by-location")
    public PageResult<WeeklyInventoryAdjustmentActivityByLocationSummaryResponse> listWeeklyAdjustmentActivityByLocationSummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listWeeklyAdjustmentActivityByLocationSummaries(
                sku,
                normalizeOptionalTransferLocationCode(locationCode, "locationCode"),
                normalizeOptionalAdjustedBy(adjustedBy),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toWeeklyAdjustmentActivityByLocationSummaryResponse);
    }

    @GetMapping("/{sku}/adjustment-activity/weekly-summary/by-adjusted-by")
    public PageResult<WeeklyInventoryAdjustmentActivityByAdjustedBySummaryResponse> listWeeklyAdjustmentActivityByAdjustedBySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listWeeklyAdjustmentActivityByAdjustedBySummaries(
                sku,
                normalizeOptionalTransferLocationCode(locationCode, "locationCode"),
                normalizeOptionalAdjustedBy(adjustedBy),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toWeeklyAdjustmentActivityByAdjustedBySummaryResponse);
    }

    @GetMapping("/{sku}/adjustment-activity/monthly-summary")
    public PageResult<MonthlyInventoryAdjustmentActivitySummaryResponse> listMonthlyAdjustmentActivitySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listMonthlyAdjustmentActivitySummaries(
                sku,
                normalizeOptionalLocationCode(locationCode),
                normalizeOptionalAdjustedBy(adjustedBy),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toMonthlyAdjustmentActivitySummaryResponse);
    }

    @GetMapping("/{sku}/adjustment-activity/monthly-summary/by-location")
    public PageResult<MonthlyInventoryAdjustmentActivityByLocationSummaryResponse> listMonthlyAdjustmentActivityByLocationSummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listMonthlyAdjustmentActivityByLocationSummaries(
                sku,
                normalizeOptionalTransferLocationCode(locationCode, "locationCode"),
                normalizeOptionalAdjustedBy(adjustedBy),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toMonthlyAdjustmentActivityByLocationSummaryResponse);
    }

    @GetMapping("/{sku}/adjustment-activity/monthly-summary/by-adjusted-by")
    public PageResult<MonthlyInventoryAdjustmentActivityByAdjustedBySummaryResponse> listMonthlyAdjustmentActivityByAdjustedBySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String locationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listMonthlyAdjustmentActivityByAdjustedBySummaries(
                sku,
                normalizeOptionalTransferLocationCode(locationCode, "locationCode"),
                normalizeOptionalAdjustedBy(adjustedBy),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toMonthlyAdjustmentActivityByAdjustedBySummaryResponse);
    }

    @PostMapping("/{sku}/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryTransferResponse transferInventory(
        @PathVariable String sku,
        @Valid @RequestBody TransferInventoryRequest request
    ) {
        InventoryTransferView transfer = inventoryAvailability.transferInventory(
            new TransferInventoryCommand(
                sku,
                request.sourceLocationCode(),
                request.destinationLocationCode(),
                request.quantity(),
                request.reason(),
                request.adjustedBy(),
                request.referenceType(),
                request.referenceId()
            )
        );
        return new InventoryTransferResponse(
            transfer.transferId(),
            transfer.sku(),
            transfer.sourceLocationCode(),
            transfer.destinationLocationCode(),
            transfer.quantity(),
            transfer.sourceOnHandQuantity(),
            transfer.destinationOnHandQuantity(),
            transfer.reason(),
            transfer.adjustedBy(),
            transfer.referenceType(),
            transfer.referenceId(),
            transfer.transferredAt()
        );
    }

    @GetMapping("/transfers/{transferId}")
    public InventoryTransferResponse transferById(@PathVariable UUID transferId) {
        return toTransferResponse(inventoryAvailability.transferById(transferId));
    }

    @PostMapping("/transfers/{transferId}/reversals")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryTransferResponse reverseTransfer(
        @PathVariable UUID transferId,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody ReverseTransferRequest request
    ) {
        return toTransferResponse(
            inventoryAvailability.reverseTransfer(
                new ReverseInventoryTransferCommand(
                    transferId,
                    request.reason(),
                    request.adjustedBy(),
                    idempotencyKey
                )
            )
        );
    }

    @GetMapping("/transfers/{transferId}/reversals")
    public PageResult<InventoryTransferResponse> listReversals(
        @PathVariable UUID transferId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return inventoryAvailability.listReversals(transferId, PageQuery.of(page, size)).map(this::toTransferResponse);
    }

    @GetMapping("/{sku}/transfers")
    public PageResult<InventoryTransferResponse> listTransfers(
        @PathVariable String sku,
        @RequestParam(required = false) String sourceLocationCode,
        @RequestParam(required = false) String destinationLocationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        String normalizedAdjustedBy = normalizeOptionalAdjustedBy(adjustedBy);
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);

        return inventoryAvailability.listTransfers(
                sku,
                normalizeOptionalTransferLocationCode(sourceLocationCode, "sourceLocationCode"),
                normalizeOptionalTransferLocationCode(destinationLocationCode, "destinationLocationCode"),
                normalizedAdjustedBy,
                normalizeOptionalReferenceType(referenceType),
                normalizeOptionalReferenceId(referenceId),
                parsedAdjustedAtFrom,
                parsedAdjustedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toTransferResponse);
    }

    @GetMapping("/{sku}/transfer-activity/daily-summary")
    public PageResult<DailyInventoryTransferActivitySummaryResponse> listDailyTransferActivitySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String sourceLocationCode,
        @RequestParam(required = false) String destinationLocationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        TransferActivityQuery query = parseTransferActivityQuery(
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo
        );

        return inventoryAvailability.listDailyTransferActivitySummaries(
                sku,
                query.sourceLocationCode(),
                query.destinationLocationCode(),
                query.adjustedBy(),
                query.referenceType(),
                query.referenceId(),
                query.adjustedAtFrom(),
                query.adjustedAtTo(),
                PageQuery.of(page, size)
            )
            .map(this::toDailyTransferActivitySummaryResponse);
    }

    @GetMapping("/{sku}/transfer-activity/weekly-summary")
    public PageResult<WeeklyInventoryTransferActivitySummaryResponse> listWeeklyTransferActivitySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String sourceLocationCode,
        @RequestParam(required = false) String destinationLocationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        TransferActivityQuery query = parseTransferActivityQuery(
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo
        );

        return inventoryAvailability.listWeeklyTransferActivitySummaries(
                sku,
                query.sourceLocationCode(),
                query.destinationLocationCode(),
                query.adjustedBy(),
                query.referenceType(),
                query.referenceId(),
                query.adjustedAtFrom(),
                query.adjustedAtTo(),
                PageQuery.of(page, size)
            )
            .map(this::toWeeklyTransferActivitySummaryResponse);
    }

    @GetMapping("/{sku}/transfer-activity/daily-summary/by-reference")
    public PageResult<DailyInventoryTransferActivityByReferenceSummaryResponse> listDailyTransferActivityByReferenceSummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String sourceLocationCode,
        @RequestParam(required = false) String destinationLocationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        TransferActivityQuery query = parseTransferActivityQuery(
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo
        );

        return inventoryAvailability.listDailyTransferActivityByReferenceSummaries(
                sku,
                query.sourceLocationCode(),
                query.destinationLocationCode(),
                query.adjustedBy(),
                query.referenceType(),
                query.referenceId(),
                query.adjustedAtFrom(),
                query.adjustedAtTo(),
                PageQuery.of(page, size)
            )
            .map(this::toDailyTransferActivityByReferenceSummaryResponse);
    }

    @GetMapping("/{sku}/transfer-activity/weekly-summary/by-reference")
    public PageResult<WeeklyInventoryTransferActivityByReferenceSummaryResponse> listWeeklyTransferActivityByReferenceSummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String sourceLocationCode,
        @RequestParam(required = false) String destinationLocationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        TransferActivityQuery query = parseTransferActivityQuery(
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo
        );

        return inventoryAvailability.listWeeklyTransferActivityByReferenceSummaries(
                sku,
                query.sourceLocationCode(),
                query.destinationLocationCode(),
                query.adjustedBy(),
                query.referenceType(),
                query.referenceId(),
                query.adjustedAtFrom(),
                query.adjustedAtTo(),
                PageQuery.of(page, size)
            )
            .map(this::toWeeklyTransferActivityByReferenceSummaryResponse);
    }

    @GetMapping("/{sku}/transfer-activity/monthly-summary")
    public PageResult<MonthlyInventoryTransferActivitySummaryResponse> listMonthlyTransferActivitySummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String sourceLocationCode,
        @RequestParam(required = false) String destinationLocationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        TransferActivityQuery query = parseTransferActivityQuery(
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo
        );

        return inventoryAvailability.listMonthlyTransferActivitySummaries(
                sku,
                query.sourceLocationCode(),
                query.destinationLocationCode(),
                query.adjustedBy(),
                query.referenceType(),
                query.referenceId(),
                query.adjustedAtFrom(),
                query.adjustedAtTo(),
                PageQuery.of(page, size)
            )
            .map(this::toMonthlyTransferActivitySummaryResponse);
    }

    @GetMapping("/{sku}/transfer-activity/monthly-summary/by-reference")
    public PageResult<MonthlyInventoryTransferActivityByReferenceSummaryResponse> listMonthlyTransferActivityByReferenceSummaries(
        @PathVariable String sku,
        @RequestParam(required = false) String sourceLocationCode,
        @RequestParam(required = false) String destinationLocationCode,
        @RequestParam(required = false) String adjustedBy,
        @RequestParam(required = false) String referenceType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(required = false) String adjustedAtFrom,
        @RequestParam(required = false) String adjustedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        TransferActivityQuery query = parseTransferActivityQuery(
            sourceLocationCode,
            destinationLocationCode,
            adjustedBy,
            referenceType,
            referenceId,
            adjustedAtFrom,
            adjustedAtTo
        );

        return inventoryAvailability.listMonthlyTransferActivityByReferenceSummaries(
                sku,
                query.sourceLocationCode(),
                query.destinationLocationCode(),
                query.adjustedBy(),
                query.referenceType(),
                query.referenceId(),
                query.adjustedAtFrom(),
                query.adjustedAtTo(),
                PageQuery.of(page, size)
            )
            .map(this::toMonthlyTransferActivityByReferenceSummaryResponse);
    }

    private InventoryAdjustmentResponse toAdjustmentResponse(InventoryAdjustmentView adjustment) {
        return new InventoryAdjustmentResponse(
            adjustment.id(),
            adjustment.sku(),
            adjustment.locationCode(),
            adjustment.previousOnHandQuantity(),
            adjustment.quantityDelta(),
            adjustment.currentOnHandQuantity(),
            adjustment.reason(),
            adjustment.adjustedBy(),
            adjustment.adjustedAt()
        );
    }

    private InventoryPickupDropoffTransactionResponse toPickupDropoffTransactionResponse(
        InventoryPickupDropoffTransactionView transaction
    ) {
        return new InventoryPickupDropoffTransactionResponse(
            transaction.id(),
            transaction.inventoryAdjustmentId(),
            transaction.sku(),
            transaction.locationCode(),
            transaction.transactionTypeCode(),
            transaction.quantity(),
            transaction.quantityDelta(),
            transaction.previousOnHandQuantity(),
            transaction.currentOnHandQuantity(),
            transaction.reason(),
            transaction.handledBy(),
            transaction.fixedAssetCode(),
            transaction.facilityCode(),
            transaction.referenceType(),
            transaction.referenceId(),
            transaction.transactionAt()
        );
    }

    private DailyInventoryPickupDropoffActivitySummaryResponse toDailyPickupDropoffActivitySummaryResponse(
        DailyInventoryPickupDropoffActivitySummaryView summary
    ) {
        return new DailyInventoryPickupDropoffActivitySummaryResponse(
            summary.sku(),
            summary.businessDate(),
            summary.locationCode(),
            summary.transactionTypeCode(),
            summary.handledBy(),
            summary.transactionCount(),
            summary.totalPickupQuantity(),
            summary.totalDropoffQuantity(),
            summary.netQuantityDelta()
        );
    }

    private WeeklyInventoryPickupDropoffActivitySummaryResponse toWeeklyPickupDropoffActivitySummaryResponse(
        WeeklyInventoryPickupDropoffActivitySummaryView summary
    ) {
        return new WeeklyInventoryPickupDropoffActivitySummaryResponse(
            summary.sku(),
            summary.businessWeekStart(),
            summary.locationCode(),
            summary.transactionTypeCode(),
            summary.handledBy(),
            summary.transactionCount(),
            summary.totalPickupQuantity(),
            summary.totalDropoffQuantity(),
            summary.netQuantityDelta()
        );
    }

    private MonthlyInventoryPickupDropoffActivitySummaryResponse toMonthlyPickupDropoffActivitySummaryResponse(
        MonthlyInventoryPickupDropoffActivitySummaryView summary
    ) {
        return new MonthlyInventoryPickupDropoffActivitySummaryResponse(
            summary.sku(),
            summary.businessMonth(),
            summary.locationCode(),
            summary.transactionTypeCode(),
            summary.handledBy(),
            summary.transactionCount(),
            summary.totalPickupQuantity(),
            summary.totalDropoffQuantity(),
            summary.netQuantityDelta()
        );
    }

    private InventoryTransferResponse toTransferResponse(InventoryTransferView transfer) {
        return new InventoryTransferResponse(
            transfer.transferId(),
            transfer.sku(),
            transfer.sourceLocationCode(),
            transfer.destinationLocationCode(),
            transfer.quantity(),
            transfer.sourceOnHandQuantity(),
            transfer.destinationOnHandQuantity(),
            transfer.reason(),
            transfer.adjustedBy(),
            transfer.referenceType(),
            transfer.referenceId(),
            transfer.transferredAt()
        );
    }

    private DailyInventoryAdjustmentActivitySummaryResponse toDailyAdjustmentActivitySummaryResponse(
        DailyInventoryAdjustmentActivitySummaryView summary
    ) {
        return new DailyInventoryAdjustmentActivitySummaryResponse(
            summary.sku(),
            summary.locationCode(),
            summary.businessDate(),
            summary.adjustmentCount(),
            summary.netQuantityDelta()
        );
    }

    private WeeklyInventoryAdjustmentActivitySummaryResponse toWeeklyAdjustmentActivitySummaryResponse(
        WeeklyInventoryAdjustmentActivitySummaryView summary
    ) {
        return new WeeklyInventoryAdjustmentActivitySummaryResponse(
            summary.sku(),
            summary.locationCode(),
            summary.businessWeekStart(),
            summary.adjustmentCount(),
            summary.netQuantityDelta()
        );
    }

    private DailyInventoryAdjustmentActivityByLocationSummaryResponse toDailyAdjustmentActivityByLocationSummaryResponse(
        DailyInventoryAdjustmentActivityByLocationSummaryView summary
    ) {
        return new DailyInventoryAdjustmentActivityByLocationSummaryResponse(
            summary.sku(),
            summary.businessDate(),
            summary.locationCode(),
            summary.adjustmentCount(),
            summary.netQuantityDelta()
        );
    }

    private WeeklyInventoryAdjustmentActivityByLocationSummaryResponse toWeeklyAdjustmentActivityByLocationSummaryResponse(
        WeeklyInventoryAdjustmentActivityByLocationSummaryView summary
    ) {
        return new WeeklyInventoryAdjustmentActivityByLocationSummaryResponse(
            summary.sku(),
            summary.businessWeekStart(),
            summary.locationCode(),
            summary.adjustmentCount(),
            summary.netQuantityDelta()
        );
    }

    private MonthlyInventoryAdjustmentActivitySummaryResponse toMonthlyAdjustmentActivitySummaryResponse(
        MonthlyInventoryAdjustmentActivitySummaryView summary
    ) {
        return new MonthlyInventoryAdjustmentActivitySummaryResponse(
            summary.sku(),
            summary.locationCode(),
            summary.businessMonth(),
            summary.adjustmentCount(),
            summary.netQuantityDelta()
        );
    }

    private MonthlyInventoryAdjustmentActivityByLocationSummaryResponse toMonthlyAdjustmentActivityByLocationSummaryResponse(
        MonthlyInventoryAdjustmentActivityByLocationSummaryView summary
    ) {
        return new MonthlyInventoryAdjustmentActivityByLocationSummaryResponse(
            summary.sku(),
            summary.businessMonth(),
            summary.locationCode(),
            summary.adjustmentCount(),
            summary.netQuantityDelta()
        );
    }

    private DailyInventoryAdjustmentActivityByAdjustedBySummaryResponse toDailyAdjustmentActivityByAdjustedBySummaryResponse(
        DailyInventoryAdjustmentActivityByAdjustedBySummaryView summary
    ) {
        return new DailyInventoryAdjustmentActivityByAdjustedBySummaryResponse(
            summary.sku(),
            summary.businessDate(),
            summary.adjustedBy(),
            summary.adjustmentCount(),
            summary.netQuantityDelta()
        );
    }

    private WeeklyInventoryAdjustmentActivityByAdjustedBySummaryResponse toWeeklyAdjustmentActivityByAdjustedBySummaryResponse(
        WeeklyInventoryAdjustmentActivityByAdjustedBySummaryView summary
    ) {
        return new WeeklyInventoryAdjustmentActivityByAdjustedBySummaryResponse(
            summary.sku(),
            summary.businessWeekStart(),
            summary.adjustedBy(),
            summary.adjustmentCount(),
            summary.netQuantityDelta()
        );
    }

    private MonthlyInventoryAdjustmentActivityByAdjustedBySummaryResponse toMonthlyAdjustmentActivityByAdjustedBySummaryResponse(
        MonthlyInventoryAdjustmentActivityByAdjustedBySummaryView summary
    ) {
        return new MonthlyInventoryAdjustmentActivityByAdjustedBySummaryResponse(
            summary.sku(),
            summary.businessMonth(),
            summary.adjustedBy(),
            summary.adjustmentCount(),
            summary.netQuantityDelta()
        );
    }

    private DailyInventoryTransferActivitySummaryResponse toDailyTransferActivitySummaryResponse(
        DailyInventoryTransferActivitySummaryView summary
    ) {
        return new DailyInventoryTransferActivitySummaryResponse(
            summary.sku(),
            summary.businessDate(),
            summary.sourceLocationCode(),
            summary.destinationLocationCode(),
            summary.adjustedBy(),
            summary.transferCount(),
            summary.totalQuantity()
        );
    }

    private WeeklyInventoryTransferActivitySummaryResponse toWeeklyTransferActivitySummaryResponse(
        WeeklyInventoryTransferActivitySummaryView summary
    ) {
        return new WeeklyInventoryTransferActivitySummaryResponse(
            summary.sku(),
            summary.businessWeekStart(),
            summary.sourceLocationCode(),
            summary.destinationLocationCode(),
            summary.adjustedBy(),
            summary.transferCount(),
            summary.totalQuantity()
        );
    }

    private MonthlyInventoryTransferActivitySummaryResponse toMonthlyTransferActivitySummaryResponse(
        MonthlyInventoryTransferActivitySummaryView summary
    ) {
        return new MonthlyInventoryTransferActivitySummaryResponse(
            summary.sku(),
            summary.businessMonth(),
            summary.sourceLocationCode(),
            summary.destinationLocationCode(),
            summary.adjustedBy(),
            summary.transferCount(),
            summary.totalQuantity()
        );
    }

    private DailyInventoryTransferActivityByReferenceSummaryResponse toDailyTransferActivityByReferenceSummaryResponse(
        DailyInventoryTransferActivityByReferenceSummaryView summary
    ) {
        return new DailyInventoryTransferActivityByReferenceSummaryResponse(
            summary.sku(),
            summary.businessDate(),
            summary.referenceType(),
            summary.referenceId(),
            summary.transferCount(),
            summary.totalQuantity()
        );
    }

    private WeeklyInventoryTransferActivityByReferenceSummaryResponse toWeeklyTransferActivityByReferenceSummaryResponse(
        WeeklyInventoryTransferActivityByReferenceSummaryView summary
    ) {
        return new WeeklyInventoryTransferActivityByReferenceSummaryResponse(
            summary.sku(),
            summary.businessWeekStart(),
            summary.referenceType(),
            summary.referenceId(),
            summary.transferCount(),
            summary.totalQuantity()
        );
    }

    private MonthlyInventoryTransferActivityByReferenceSummaryResponse toMonthlyTransferActivityByReferenceSummaryResponse(
        MonthlyInventoryTransferActivityByReferenceSummaryView summary
    ) {
        return new MonthlyInventoryTransferActivityByReferenceSummaryResponse(
            summary.sku(),
            summary.businessMonth(),
            summary.referenceType(),
            summary.referenceId(),
            summary.transferCount(),
            summary.totalQuantity()
        );
    }

    private static String normalizeOptionalLocationCode(String locationCode) {
        if (locationCode == null) {
            return DEFAULT_LOCATION_CODE;
        }
        if (locationCode.isBlank()) {
            throw new IllegalArgumentException("locationCode query parameter must not be blank");
        }
        return locationCode.trim().toUpperCase();
    }

    private static String normalizeOptionalTransferLocationCode(String locationCode, String parameterName) {
        if (locationCode == null) {
            return null;
        }
        if (locationCode.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        return locationCode.trim().toUpperCase();
    }

    private static String normalizeOptionalAdjustedBy(String adjustedBy) {
        if (adjustedBy == null) {
            return null;
        }
        if (adjustedBy.isBlank()) {
            throw new IllegalArgumentException("adjustedBy query parameter must not be blank");
        }
        return adjustedBy.trim().toLowerCase();
    }

    private static String normalizeOptionalHandledBy(String handledBy) {
        if (handledBy == null) {
            return null;
        }
        if (handledBy.isBlank()) {
            throw new IllegalArgumentException("handledBy query parameter must not be blank");
        }
        return handledBy.trim().toLowerCase();
    }

    private static String normalizeOptionalFixedAssetCode(String fixedAssetCode) {
        if (fixedAssetCode == null) {
            return null;
        }
        if (fixedAssetCode.isBlank()) {
            throw new IllegalArgumentException("fixedAssetCode query parameter must not be blank");
        }
        return fixedAssetCode.trim().toUpperCase();
    }

    private static String normalizeOptionalFacilityCode(String facilityCode) {
        if (facilityCode == null) {
            return null;
        }
        if (facilityCode.isBlank()) {
            throw new IllegalArgumentException("facilityCode query parameter must not be blank");
        }
        return facilityCode.trim().toUpperCase();
    }

    private static String normalizeOptionalTransactionTypeCode(String transactionTypeCode) {
        if (transactionTypeCode == null) {
            return null;
        }
        if (transactionTypeCode.isBlank()) {
            throw new IllegalArgumentException("transactionTypeCode query parameter must not be blank");
        }
        String normalizedTransactionTypeCode = transactionTypeCode.trim().toUpperCase();
        if (!"PICKUP".equals(normalizedTransactionTypeCode) && !"DROPOFF".equals(normalizedTransactionTypeCode)) {
            throw new IllegalArgumentException("transactionTypeCode must be PICKUP or DROPOFF");
        }
        return normalizedTransactionTypeCode;
    }

    private static String normalizeOptionalReferenceType(String referenceType) {
        if (referenceType == null) {
            return null;
        }
        if (referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType query parameter must not be blank");
        }
        return referenceType.trim().toUpperCase();
    }

    private static String normalizeOptionalReferenceId(String referenceId) {
        if (referenceId == null) {
            return null;
        }
        if (referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId query parameter must not be blank");
        }
        return referenceId.trim();
    }

    private static TransferActivityQuery parseTransferActivityQuery(
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        String adjustedAtFrom,
        String adjustedAtTo
    ) {
        Instant parsedAdjustedAtFrom = parseOptionalInstant(adjustedAtFrom, "adjustedAtFrom");
        Instant parsedAdjustedAtTo = parseOptionalInstant(adjustedAtTo, "adjustedAtTo");
        validateAdjustedAtRange(parsedAdjustedAtFrom, parsedAdjustedAtTo);
        return new TransferActivityQuery(
            normalizeOptionalTransferLocationCode(sourceLocationCode, "sourceLocationCode"),
            normalizeOptionalTransferLocationCode(destinationLocationCode, "destinationLocationCode"),
            normalizeOptionalAdjustedBy(adjustedBy),
            normalizeOptionalReferenceType(referenceType),
            normalizeOptionalReferenceId(referenceId),
            parsedAdjustedAtFrom,
            parsedAdjustedAtTo
        );
    }

    private static PickupDropoffActivityQuery parsePickupDropoffActivityQuery(
        String locationCode,
        String transactionTypeCode,
        String handledBy,
        String fixedAssetCode,
        String facilityCode,
        String referenceType,
        String referenceId,
        String transactionAtFrom,
        String transactionAtTo
    ) {
        Instant parsedTransactionAtFrom = parseOptionalInstant(transactionAtFrom, "transactionAtFrom");
        Instant parsedTransactionAtTo = parseOptionalInstant(transactionAtTo, "transactionAtTo");
        validateTransactionAtRange(parsedTransactionAtFrom, parsedTransactionAtTo);
        return new PickupDropoffActivityQuery(
            normalizeOptionalTransferLocationCode(locationCode, "locationCode"),
            normalizeOptionalTransactionTypeCode(transactionTypeCode),
            normalizeOptionalHandledBy(handledBy),
            normalizeOptionalFixedAssetCode(fixedAssetCode),
            normalizeOptionalFacilityCode(facilityCode),
            normalizeOptionalReferenceType(referenceType),
            normalizeOptionalReferenceId(referenceId),
            parsedTransactionAtFrom,
            parsedTransactionAtTo
        );
    }

    private record TransferActivityQuery(
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo
    ) {
    }

    private record PickupDropoffActivityQuery(
        String locationCode,
        String transactionTypeCode,
        String handledBy,
        String fixedAssetCode,
        String facilityCode,
        String referenceType,
        String referenceId,
        Instant transactionAtFrom,
        Instant transactionAtTo
    ) {
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be a valid ISO-8601 instant");
        }
    }

    private static void validateAdjustedAtRange(Instant adjustedAtFrom, Instant adjustedAtTo) {
        if (adjustedAtFrom != null && adjustedAtTo != null && adjustedAtFrom.isAfter(adjustedAtTo)) {
            throw new IllegalArgumentException("adjustedAtFrom must be before or equal to adjustedAtTo");
        }
    }

    private static void validateTransactionAtRange(Instant transactionAtFrom, Instant transactionAtTo) {
        if (transactionAtFrom != null && transactionAtTo != null && transactionAtFrom.isAfter(transactionAtTo)) {
            throw new IllegalArgumentException("transactionAtFrom must be before or equal to transactionAtTo");
        }
    }
}
