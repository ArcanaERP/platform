package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface InventoryAvailability {

    InventoryItemView inventoryForSku(String sku, String locationCode);

    InventoryAdjustmentView adjustInventory(AdjustInventoryCommand command);

    InventoryTransferView transferInventory(TransferInventoryCommand command);

    InventoryTransferView transferById(UUID transferId);

    InventoryTransferView reverseTransfer(ReverseInventoryTransferCommand command);

    PageResult<InventoryTransferView> listReversals(UUID transferId, PageQuery pageQuery);

    PageResult<InventoryTransferView> listTransfers(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyInventoryTransferActivitySummaryView> listDailyTransferActivitySummaries(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyInventoryTransferActivitySummaryView> listWeeklyTransferActivitySummaries(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyInventoryTransferActivitySummaryView> listMonthlyTransferActivitySummaries(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryAdjustmentView> listAdjustments(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyInventoryAdjustmentActivitySummaryView> listDailyAdjustmentActivitySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyInventoryAdjustmentActivityByLocationSummaryView> listDailyAdjustmentActivityByLocationSummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyInventoryAdjustmentActivityByAdjustedBySummaryView> listDailyAdjustmentActivityByAdjustedBySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyInventoryAdjustmentActivitySummaryView> listWeeklyAdjustmentActivitySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyInventoryAdjustmentActivityByLocationSummaryView> listWeeklyAdjustmentActivityByLocationSummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyInventoryAdjustmentActivityByAdjustedBySummaryView> listWeeklyAdjustmentActivityByAdjustedBySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyInventoryAdjustmentActivitySummaryView> listMonthlyAdjustmentActivitySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyInventoryAdjustmentActivityByAdjustedBySummaryView> listMonthlyAdjustmentActivityByAdjustedBySummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyInventoryAdjustmentActivityByLocationSummaryView> listMonthlyAdjustmentActivityByLocationSummaries(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );
}
