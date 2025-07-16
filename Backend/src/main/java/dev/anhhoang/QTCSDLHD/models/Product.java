package dev.anhhoang.QTCSDLHD.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Data
@Document(collection = "products")
public class Product {
    @Id
    private String _id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String image_url;
    private String category;
    @Field("shop_id")
    private String shopid;
    @Field("shop_name")
    private String shopname;
    @CreatedDate
    private LocalDateTime created_at;
    @LastModifiedDate
    private LocalDateTime updated_at;
}