package dev.anhhoang.QTCSDLHD.models;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String _id;
    @Field("customer_id")
    private String customer_id;
    private String fullName;
    private String phoneNumber;
    private String shipping_address;
    private String payment_method;
    private BankAccount bankAccount;
    private String status;
    private double total;
    private List<OrderItem> items;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}