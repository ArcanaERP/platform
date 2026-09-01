package com.arcanaerp.platform.orders;

import java.time.LocalDate;

public record WeeklyOrderStatusActivityByCurrentStatusSummaryView(
    LocalDate businessWeekStart,
    OrderStatus currentStatus,
    long transitionCount,
    long orderCount
) {
}
