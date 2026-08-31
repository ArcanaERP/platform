package com.arcanaerp.platform.orders;

import java.time.LocalDate;

public record DailyOrderStatusActivitySummaryView(
    LocalDate businessDate,
    long transitionCount,
    long orderCount
) {
}
