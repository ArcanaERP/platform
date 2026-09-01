package com.arcanaerp.platform.orders.web;

import com.arcanaerp.platform.orders.OrderStatus;
import java.time.LocalDate;

public record WeeklyOrderStatusActivityByCurrentStatusSummaryResponse(
    LocalDate businessWeekStart,
    OrderStatus currentStatus,
    long transitionCount,
    long orderCount
) {
}
