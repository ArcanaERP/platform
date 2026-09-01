package com.arcanaerp.platform.orders.web;

import com.arcanaerp.platform.orders.OrderStatus;
import java.time.LocalDate;

public record DailyOrderStatusActivityByCurrentStatusSummaryResponse(
    LocalDate businessDate,
    OrderStatus currentStatus,
    long transitionCount,
    long orderCount
) {
}
