package dev.anhhoang.QTCSDLHD.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private String product_id;
    private Integer quantity;
    private Double price;
    private String voucherId;
    private String product_name;
    private String image_url;
}
