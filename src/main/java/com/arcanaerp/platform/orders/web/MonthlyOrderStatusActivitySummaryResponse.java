package com.arcanaerp.platform.orders.web;

import java.time.YearMonth;

public record MonthlyOrderStatusActivitySummaryResponse(
    YearMonth businessMonth,
    long transitionCount,
    long orderCount
) {
}
