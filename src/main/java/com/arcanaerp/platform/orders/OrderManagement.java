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
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyOrderStatusActivityByCurrentStatusSummaryView> listDailyStatusActivityByCurrentStatusSummaries(
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyOrderStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyOrderStatusActivityByCurrentStatusSummaryView> listWeeklyStatusActivityByCurrentStatusSummaries(
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyOrderStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyOrderStatusActivityByCurrentStatusSummaryView> listMonthlyStatusActivityByCurrentStatusSummaries(
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );
}
