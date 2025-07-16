package dev.anhhoang.QTCSDLHD.models;

import lombok.Data;

@Data
public class OrderItem {
    private String product_id;
    private Integer quantity;
    private Double price;
    private String voucherId;
}