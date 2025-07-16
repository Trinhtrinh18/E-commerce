package dev.anhhoang.QTCSDLHD.services;

import java.util.List;
import java.util.Map;

import dev.anhhoang.QTCSDLHD.dto.OrderDTO;
import dev.anhhoang.QTCSDLHD.models.Order;

public interface OrderService {
    List<Order> getOrdersBySeller(String sellerId, String status, String startDate, String endDate);
    Order getOrderDetailForSeller(String orderId, String sellerId);
    Order updateOrderStatusForSeller(String orderId, String sellerId, String newStatus);
    Map<String, Object> getOrderStatisticsBySeller(String sellerId);
    List<Map<String, Object>> getOrderChartDataBySeller(String sellerId, String period);

    // Thêm các phương thức cho buyer
    List<OrderDTO> getOrdersByCustomer(String customerId, String status);
    OrderDTO getOrderDetailForCustomer(String orderId, String customerId);

    boolean confirmOrderDelivered(String orderId, String customerId);
    // Customer cancels their own order
    boolean cancelOrderByCustomer(String orderId, String customerId);
}
