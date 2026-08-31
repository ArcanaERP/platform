package com.arcanaerp.platform.orders;

import java.time.LocalDate;

public record WeeklyOrderStatusActivitySummaryView(
    LocalDate businessWeekStart,
    long transitionCount,
    long orderCount
) {
}
