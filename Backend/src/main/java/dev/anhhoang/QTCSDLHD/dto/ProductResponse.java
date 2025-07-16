package dev.anhhoang.QTCSDLHD.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String _id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String image_url;
    private String category;
    private String shop_id;
    private String shop_name;
    private Integer quantity;
    private Integer purchaseCount;
    private Boolean viewed;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private int interactionCount;
}