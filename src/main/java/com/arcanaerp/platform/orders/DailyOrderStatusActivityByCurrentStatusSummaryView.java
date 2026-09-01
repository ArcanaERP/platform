package com.arcanaerp.platform.orders;

import java.time.LocalDate;

public record DailyOrderStatusActivityByCurrentStatusSummaryView(
    LocalDate businessDate,
    OrderStatus currentStatus,
    long transitionCount,
    long orderCount
) {
}
