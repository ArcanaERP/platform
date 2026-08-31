package com.arcanaerp.platform.orders.web;

import java.time.LocalDate;

public record WeeklyOrderStatusActivitySummaryResponse(
    LocalDate businessWeekStart,
    long transitionCount,
    long orderCount
) {
}
