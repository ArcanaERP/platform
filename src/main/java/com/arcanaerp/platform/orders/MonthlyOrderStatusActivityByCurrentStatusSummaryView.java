package com.arcanaerp.platform.orders;

import java.time.YearMonth;

public record MonthlyOrderStatusActivityByCurrentStatusSummaryView(
    YearMonth businessMonth,
    OrderStatus currentStatus,
    long transitionCount,
    long orderCount
) {
}
