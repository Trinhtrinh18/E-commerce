package dev.anhhoang.QTCSDLHD.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class OrderDTO {
    private String _id;
    private String customer_id;
    private String fullName;
    private String phoneNumber;
    private String shipping_address;
    private String payment_method;
    private String status;
    private double total;
    private List<OrderItemDTO> items;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
