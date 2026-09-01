package com.arcanaerp.platform.orders.web;

import com.arcanaerp.platform.orders.OrderStatus;
import java.time.YearMonth;

public record MonthlyOrderStatusActivityByCurrentStatusSummaryResponse(
    YearMonth businessMonth,
    OrderStatus currentStatus,
    long transitionCount,
    long orderCount
) {
}
