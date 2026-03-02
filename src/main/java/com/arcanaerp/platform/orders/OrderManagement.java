package com.arcanaerp.platform.orders;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface OrderManagement {

    OrderView createOrder(CreateOrderCommand command);

    OrderView getOrder(String orderNumber);

    PageResult<OrderView> listOrders(PageQuery pageQuery);

    OrderView changeOrderStatus(ChangeOrderStatusCommand command);

    PageResult<OrderStatusChangeView> listStatusHistory(
        String orderNumber,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );
}
