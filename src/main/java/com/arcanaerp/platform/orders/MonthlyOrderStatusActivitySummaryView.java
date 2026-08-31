package com.arcanaerp.platform.orders;

import java.time.YearMonth;

public record MonthlyOrderStatusActivitySummaryView(
    YearMonth businessMonth,
    long transitionCount,
    long orderCount
) {
}
