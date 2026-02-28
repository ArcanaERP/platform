package com.arcanaerp.platform.orders;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface OrderManagement {

    OrderView createOrder(CreateOrderCommand command);

    PageResult<OrderView> listOrders(PageQuery pageQuery);
}
