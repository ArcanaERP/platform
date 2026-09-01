package com.arcanaerp.platform.orders;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface OrderManagement {

    OrderView createOrder(CreateOrderCommand command);

    OrderView getOrder(String orderNumber);

    PageResult<OrderView> listOrders(PageQuery pageQuery);

    PageResult<OrderView> listOrders(String tenantCode, PageQuery pageQuery);

    OrderView changeOrderStatus(ChangeOrderStatusCommand command);

    PageResult<OrderStatusChangeView> listStatusHistory(
        String orderNumber,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyOrderStatusActivitySummaryView> listDailyStatusActivitySummaries(
        String tenantCode,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyOrderStatusActivityByCurrentStatusSummaryView> listDailyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyOrderStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        String tenantCode,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyOrderStatusActivityByCurrentStatusSummaryView> listWeeklyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyOrderStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        String tenantCode,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyOrderStatusActivityByCurrentStatusSummaryView> listMonthlyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );
}
